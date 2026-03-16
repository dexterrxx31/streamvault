package com.streamvault.service;

import com.streamvault.dto.VideoResponse;
import com.streamvault.model.User;
import com.streamvault.model.Video;
import com.streamvault.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path uploadPath;

    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
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

    public Resource getVideoResource(Long id) {
        Video video = getVideoById(id);
        try {
            Path filePath = uploadPath.resolve(video.getFilename()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            }
            throw new RuntimeException("Video file not found");
        } catch (IOException e) {
            throw new RuntimeException("Error reading video file", e);
        }
    }

    public void deleteVideo(Long id, Long userId) {
        Video video = getVideoById(id);
        if (!video.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this video");
        }
        try {
            Path filePath = uploadPath.resolve(video.getFilename());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't fail
        }
        videoRepository.delete(video);
    }

    private VideoResponse toResponse(Video video) {
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .contentType(video.getContentType())
                .size(video.getSize())
                .uploadDate(video.getUploadDate())
                .build();
    }
}
