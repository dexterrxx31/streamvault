package com.streamvault.controller;

import com.streamvault.dto.UpdateVideoRequest;
import com.streamvault.dto.VideoResponse;
import com.streamvault.model.User;
import com.streamvault.model.Video;
import com.streamvault.service.AuthService;
import com.streamvault.service.VideoService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @GetMapping("/{id}")
    public ResponseEntity<?> getVideo(@PathVariable Long id, Authentication authentication) {
        try {
            User user = authService.getUserByUsername(authentication.getName());
            VideoResponse response = videoService.getUserVideo(id, user.getId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVideo(@PathVariable Long id,
            @RequestBody UpdateVideoRequest request,
            Authentication authentication) {
        try {
            User user = authService.getUserByUsername(authentication.getName());
            VideoResponse response = videoService.updateVideo(id, user.getId(),
                    request.getTitle(), request.getDescription(), request.getTags());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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

    // Public like /stream/** — <img> tags cannot send Authorization headers.
    // Accepted tradeoff: numeric ids are guessable; tightening would need signed URLs.
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<?> getThumbnail(@PathVariable Long id) {
        try {
            Resource resource = videoService.getThumbnailResource(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stream/{id}")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long id) {
        try {
            Video video = videoService.getVideoById(id);
            Resource resource = videoService.getVideoResource(id);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(video.getContentType()))
                    .contentLength(resource.contentLength())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Selected over streamVideo() when a Range header is present. ResourceRegion
    // streams the requested slice from disk instead of loading it into memory;
    // Spring's ResourceRegionHttpMessageConverter writes Content-Range for us.
    @GetMapping(value = "/stream/{id}", headers = "Range")
    public ResponseEntity<ResourceRegion> streamVideoRange(@PathVariable Long id,
            @RequestHeader("Range") String rangeHeader) {
        try {
            Video video = videoService.getVideoById(id);
            Resource resource = videoService.getVideoResource(id);

            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            ResourceRegion region = HttpRange.toResourceRegions(ranges, resource).get(0);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.parseMediaType(video.getContentType()))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(region);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        }
    }
}
