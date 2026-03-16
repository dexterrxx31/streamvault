package com.streamvault.controller;

import com.streamvault.dto.VideoResponse;
import com.streamvault.model.User;
import com.streamvault.model.Video;
import com.streamvault.service.AuthService;
import com.streamvault.service.VideoService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;
    private final AuthService authService;

    public VideoController(VideoService videoService, AuthService authService) {
        this.videoService = videoService;
        this.authService = authService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            Authentication authentication) {
        try {
            User user = authService.getUserByUsername(authentication.getName());
            VideoResponse response = videoService.uploadVideo(file, title, user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<VideoResponse>> getUserVideos(Authentication authentication) {
        User user = authService.getUserByUsername(authentication.getName());
        List<VideoResponse> videos = videoService.getUserVideos(user.getId());
        return ResponseEntity.ok(videos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long id, Authentication authentication) {
        try {
            User user = authService.getUserByUsername(authentication.getName());
            videoService.deleteVideo(id, user.getId());
            return ResponseEntity.ok(Map.of("message", "Video deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stream/{id}")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long id,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            Video video = videoService.getVideoById(id);
            Resource resource = videoService.getVideoResource(id);
            long fileLength = resource.contentLength();

            if (rangeHeader == null) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(video.getContentType()))
                        .contentLength(fileLength)
                        .body(resource);
            }

            // Parse range header
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            long rangeStart = Long.parseLong(ranges[0]);
            long rangeEnd = ranges.length > 1 && !ranges[1].isEmpty()
                    ? Long.parseLong(ranges[1])
                    : fileLength - 1;

            if (rangeEnd > fileLength - 1) {
                rangeEnd = fileLength - 1;
            }

            long contentLength = rangeEnd - rangeStart + 1;

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
            headers.add("Accept-Ranges", "bytes");
            headers.setContentLength(contentLength);
            headers.setContentType(MediaType.parseMediaType(video.getContentType()));

            java.io.InputStream inputStream = resource.getInputStream();
            inputStream.skip(rangeStart);
            byte[] data = inputStream.readNBytes((int) contentLength);
            inputStream.close();

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(new org.springframework.core.io.ByteArrayResource(data));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
