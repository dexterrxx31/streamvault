package com.streamvault.service;

import com.streamvault.dto.VideoResponse;
import com.streamvault.model.User;
import com.streamvault.model.Video;
import com.streamvault.repository.VideoRepository;
import com.streamvault.service.processing.VideoUploadedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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

    private final VideoRepository videoRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path uploadPath;

    public VideoService(VideoRepository videoRepository, ApplicationEventPublisher eventPublisher) {
        this.videoRepository = videoRepository;
        this.eventPublisher = eventPublisher;
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
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFilename = UUID.randomUUID().toString() + extension;

            Path targetLocation = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            Video video = Video.builder()
                    .title(title)
                    .filename(storedFilename)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .user(user)
                    .build();

            Video saved = videoRepository.save(video);

            // Kick off async post-processing (ffprobe metadata, thumbnails, AI steps)
            eventPublisher.publishEvent(new VideoUploadedEvent(saved.getId()));

            return toResponse(saved);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload video", e);
        }
    }

    public List<VideoResponse> getUserVideos(Long userId) {
        return videoRepository.findByUserIdOrderByUploadDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Video getVideoById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
    }

    public VideoResponse getUserVideo(Long id, Long userId) {
        Video video = getVideoById(id);
        if (!video.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to view this video");
        }
        return toResponse(video);
    }

    public VideoResponse updateVideo(Long id, Long userId, String title, String description, List<String> tags) {
        Video video = getVideoById(id);
        if (!video.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this video");
        }
        if (title != null && !title.isBlank()) {
            video.setTitle(title);
        }
        if (description != null) {
            video.setDescription(description);
        }
        if (tags != null) {
            video.setTags(tags);
        }
        return toResponse(videoRepository.save(video));
    }

    public Resource getVideoResource(Long id) {
        Video video = getVideoById(id);
        return fileResource(video.getFilename(), "Video file not found");
    }

    public Resource getThumbnailResource(Long id) {
        Video video = getVideoById(id);
        if (video.getThumbnailFilename() == null) {
            throw new RuntimeException("Thumbnail not available");
        }
        return fileResource(video.getThumbnailFilename(), "Thumbnail file not found");
    }

    public void deleteVideo(Long id, Long userId) {
        Video video = getVideoById(id);
        if (!video.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this video");
        }
        deleteQuietly(video.getFilename());
        if (video.getThumbnailFilename() != null) {
            deleteQuietly(video.getThumbnailFilename());
        }
        // Frames extracted for AI analysis follow a naming convention next to the video
        String baseName = stripExtension(video.getFilename());
        for (int i = 1; i <= 4; i++) {
            deleteQuietly(baseName + "_frame_" + i + ".jpg");
        }
        videoRepository.delete(video);
    }

    private Resource fileResource(String filename, String notFoundMessage) {
        try {
            Path filePath = uploadPath.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            }
            throw new RuntimeException(notFoundMessage);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }

    private void deleteQuietly(String filename) {
        try {
            Files.deleteIfExists(uploadPath.resolve(filename));
        } catch (IOException e) {
            // Log but don't fail
        }
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private VideoResponse toResponse(Video video) {
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .contentType(video.getContentType())
                .size(video.getSize())
                .uploadDate(video.getUploadDate())
                .durationSeconds(video.getDurationSeconds())
                .width(video.getWidth())
                .height(video.getHeight())
                .description(video.getDescription())
                .tags(video.getTags())
                .status(video.getStatus())
                .hasThumbnail(video.getThumbnailFilename() != null)
                .build();
    }
}
