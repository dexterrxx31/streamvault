package com.streamvault.service;

import com.streamvault.dto.ChapterResponse;
import com.streamvault.dto.VideoResponse;
import com.streamvault.exception.BadRequestException;
import com.streamvault.exception.NotFoundException;
import com.streamvault.exception.ServiceUnavailableException;
import com.streamvault.model.User;
import com.streamvault.model.Video;
import com.streamvault.repository.VideoRepository;
import com.streamvault.security.MediaUrlSigner;
import com.streamvault.service.processing.MediaFiles;
import com.streamvault.service.processing.VideoUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoService {

    public static final String KIND_STREAM = "stream";
    public static final String KIND_THUMBNAIL = "thumbnail";
    public static final String KIND_CAPTIONS = "captions";

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);
    private static final int MAX_TITLE_LENGTH = 255;

    private final VideoRepository videoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MediaUrlSigner mediaUrlSigner;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path uploadPath;

    public VideoService(VideoRepository videoRepository, ApplicationEventPublisher eventPublisher,
            MediaUrlSigner mediaUrlSigner) {
        this.videoRepository = videoRepository;
        this.eventPublisher = eventPublisher;
        this.mediaUrlSigner = mediaUrlSigner;
    }

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public VideoResponse uploadVideo(MultipartFile file, String title, User user) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new BadRequestException("Title must be at most " + MAX_TITLE_LENGTH + " characters");
        }
        // The client's filename and Content-Type are untrusted: only a known
        // extension is kept, and the served content type is derived from it
        String extension = MediaFiles.allowedExtension(file.getOriginalFilename())
                .orElseThrow(() -> new BadRequestException("Unsupported file type. Allowed: MP4, M4V, WebM, MOV, MKV, AVI"));
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path targetLocation = uploadPath.resolve(storedFilename);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store uploaded video", e);
        }

        Video saved;
        try {
            saved = videoRepository.save(Video.builder()
                    .title(title.trim())
                    .filename(storedFilename)
                    .contentType(MediaFiles.contentTypeFor(storedFilename))
                    .size(file.getSize())
                    .user(user)
                    .build());
        } catch (RuntimeException e) {
            deleteQuietly(storedFilename);
            throw e;
        }

        try {
            // Kick off async post-processing (ffprobe metadata, thumbnails, AI steps)
            eventPublisher.publishEvent(new VideoUploadedEvent(saved.getId()));
        } catch (TaskRejectedException e) {
            // Processing queue is full — undo the upload rather than leave it stuck in PROCESSING
            videoRepository.delete(saved);
            deleteQuietly(storedFilename);
            throw new ServiceUnavailableException("Server is busy processing other videos, please try again later");
        }

        return toResponse(saved);
    }

    public List<VideoResponse> getUserVideos(Long userId) {
        return videoRepository.findByUserIdOrderByUploadDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Video getVideoById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Video not found"));
    }

    /** Loads a video the user owns; someone else's video looks exactly like a missing one. */
    private Video getOwnedVideo(Long id, Long userId) {
        Video video = getVideoById(id);
        if (!video.getUser().getId().equals(userId)) {
            throw new NotFoundException("Video not found");
        }
        return video;
    }

    public VideoResponse getUserVideo(Long id, Long userId) {
        return toResponse(getOwnedVideo(id, userId));
    }

    public VideoResponse updateVideo(Long id, Long userId, String title, String description, List<String> tags) {
        Video video = getOwnedVideo(id, userId);
        if (title != null && !title.isBlank()) {
            video.setTitle(title);
        }
        if (description != null) {
            video.setDescription(description);
        }
        if (tags != null) {
            video.setTags(tags);
        }
        // Any update (accept, edit, or dismiss) resolves the AI suggestions
        video.setAiTitle(null);
        video.setAiDescription(null);
        video.setAiTags(null);
        return toResponse(videoRepository.save(video));
    }

    public Resource getVideoResource(Long id) {
        Video video = getVideoById(id);
        return fileResource(video.getFilename());
    }

    public Resource getThumbnailResource(Long id) {
        Video video = getVideoById(id);
        if (video.getThumbnailFilename() == null) {
            throw new NotFoundException("Thumbnail not available");
        }
        return fileResource(video.getThumbnailFilename());
    }

    public Resource getCaptionsResource(Long id) {
        Video video = getVideoById(id);
        if (video.getCaptionsFilename() == null) {
            throw new NotFoundException("Captions not available");
        }
        return fileResource(video.getCaptionsFilename());
    }

    public boolean isValidMediaSignature(Long id, String kind, Long exp, String sig) {
        return mediaUrlSigner.verify(id, kind, exp, sig);
    }

    public void deleteVideo(Long id, Long userId) {
        Video video = getOwnedVideo(id, userId);
        deleteQuietly(video.getFilename());
        MediaFiles.derivedFilenames(video.getFilename()).forEach(this::deleteQuietly);
        videoRepository.delete(video);
    }

    private Resource fileResource(String filename) {
        try {
            Path filePath = uploadPath.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (filePath.startsWith(uploadPath) && resource.exists()) {
                return resource;
            }
            throw new NotFoundException("File not found");
        } catch (IOException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }

    private void deleteQuietly(String filename) {
        try {
            Files.deleteIfExists(uploadPath.resolve(filename));
        } catch (IOException e) {
            log.warn("Could not delete {}", filename, e);
        }
    }

    private String signedUrl(Video video, String path, String kind) {
        return "/api/videos/" + path + "?" + mediaUrlSigner.signQuery(video.getId(), kind);
    }

    private VideoResponse toResponse(Video video) {
        boolean hasThumbnail = video.getThumbnailFilename() != null;
        boolean hasCaptions = video.getCaptionsFilename() != null;
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .contentType(MediaFiles.contentTypeFor(video.getFilename()))
                .size(video.getSize())
                .uploadDate(video.getUploadDate())
                .durationSeconds(video.getDurationSeconds())
                .width(video.getWidth())
                .height(video.getHeight())
                .description(video.getDescription())
                .tags(video.getTags())
                .status(video.getStatus())
                .hasThumbnail(hasThumbnail)
                .aiTitle(video.getAiTitle())
                .aiDescription(video.getAiDescription())
                .aiTags(video.getAiTags() != null
                        ? List.of(video.getAiTags().split(","))
                        : List.of())
                .summary(video.getSummary())
                .chapters(video.getChapters().stream()
                        .map(c -> new ChapterResponse(c.getStartSeconds(), c.getTitle()))
                        .collect(Collectors.toList()))
                .hasCaptions(hasCaptions)
                .streamUrl(signedUrl(video, "stream/" + video.getId(), KIND_STREAM))
                .thumbnailUrl(hasThumbnail ? signedUrl(video, video.getId() + "/thumbnail", KIND_THUMBNAIL) : null)
                .captionsUrl(hasCaptions ? signedUrl(video, video.getId() + "/captions.vtt", KIND_CAPTIONS) : null)
                .build();
    }
}
