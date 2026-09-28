package com.streamvault.controller;

import com.streamvault.dto.UpdateVideoRequest;
import com.streamvault.dto.VideoResponse;
import com.streamvault.exception.NotFoundException;
import com.streamvault.model.User;
import com.streamvault.model.Video;
import com.streamvault.service.AuthService;
import com.streamvault.service.VideoService;
import com.streamvault.service.processing.MediaFiles;
import jakarta.validation.Valid;
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
    public ResponseEntity<VideoResponse> uploadVideo(@RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            Authentication authentication) {
        User user = authService.getUserByUsername(authentication.getName());
        return ResponseEntity.ok(videoService.uploadVideo(file, title, user));
    }

    @GetMapping
    public ResponseEntity<List<VideoResponse>> getUserVideos(Authentication authentication) {
        User user = authService.getUserByUsername(authentication.getName());
        return ResponseEntity.ok(videoService.getUserVideos(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long id, Authentication authentication) {
        User user = authService.getUserByUsername(authentication.getName());
        return ResponseEntity.ok(videoService.getUserVideo(id, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VideoResponse> updateVideo(@PathVariable Long id,
            @Valid @RequestBody UpdateVideoRequest request,
            Authentication authentication) {
        User user = authService.getUserByUsername(authentication.getName());
        return ResponseEntity.ok(videoService.updateVideo(id, user.getId(),
                request.getTitle(), request.getDescription(), request.getTags()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteVideo(@PathVariable Long id, Authentication authentication) {
        User user = authService.getUserByUsername(authentication.getName());
        videoService.deleteVideo(id, user.getId());
        return ResponseEntity.ok(Map.of("message", "Video deleted successfully"));
    }

    // Media endpoints below are reachable without a JWT because <video>, <img>
    // and <track> tags cannot send Authorization headers. Access is instead
    // gated by the exp/sig pair in the pre-signed URL the owner receives in
    // VideoResponse; a missing or invalid signature is indistinguishable from
    // a missing video.

    @GetMapping("/{id}/captions.vtt")
    public ResponseEntity<Resource> getCaptions(@PathVariable Long id,
            @RequestParam(required = false) Long exp, @RequestParam(required = false) String sig) {
        requireSignature(id, VideoService.KIND_CAPTIONS, exp, sig);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/vtt;charset=UTF-8"))
                .body(videoService.getCaptionsResource(id));
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(@PathVariable Long id,
            @RequestParam(required = false) Long exp, @RequestParam(required = false) String sig) {
        requireSignature(id, VideoService.KIND_THUMBNAIL, exp, sig);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(videoService.getThumbnailResource(id));
    }

    @GetMapping("/stream/{id}")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long id,
            @RequestParam(required = false) Long exp, @RequestParam(required = false) String sig)
            throws IOException {
        requireSignature(id, VideoService.KIND_STREAM, exp, sig);
        Video video = videoService.getVideoById(id);
        Resource resource = videoService.getVideoResource(id);

        return ResponseEntity.ok()
                .headers(mediaHeaders(video))
                .contentLength(resource.contentLength())
                .body(resource);
    }

    // Selected over streamVideo() when a Range header is present. ResourceRegion
    // streams the requested slice from disk instead of loading it into memory;
    // Spring's ResourceRegionHttpMessageConverter writes Content-Range for us.
    @GetMapping(value = "/stream/{id}", headers = "Range")
    public ResponseEntity<ResourceRegion> streamVideoRange(@PathVariable Long id,
            @RequestHeader("Range") String rangeHeader,
            @RequestParam(required = false) Long exp, @RequestParam(required = false) String sig) {
        requireSignature(id, VideoService.KIND_STREAM, exp, sig);
        Video video = videoService.getVideoById(id);
        Resource resource = videoService.getVideoResource(id);

        try {
            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            ResourceRegion region = HttpRange.toResourceRegions(ranges, resource).get(0);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(mediaHeaders(video))
                    .body(region);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        }
    }

    private void requireSignature(Long id, String kind, Long exp, String sig) {
        if (!videoService.isValidMediaSignature(id, kind, exp, sig)) {
            throw new NotFoundException("Video not found");
        }
    }

    /**
     * Content type comes from the server-chosen file extension (never the
     * client's upload header), and nosniff + inline disposition stop a
     * browser from rendering anything but media.
     */
    private HttpHeaders mediaHeaders(Video video) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(MediaFiles.contentTypeFor(video.getFilename())));
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setContentDisposition(ContentDisposition.inline().build());
        return headers;
    }
}
