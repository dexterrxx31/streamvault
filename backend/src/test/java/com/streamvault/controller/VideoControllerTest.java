package com.streamvault.controller;

import com.streamvault.config.WebConfig;
import com.streamvault.dto.VideoResponse;
import com.streamvault.exception.BadRequestException;
import com.streamvault.exception.NotFoundException;
import com.streamvault.model.User;
import com.streamvault.model.Video;
import com.streamvault.model.VideoStatus;
import com.streamvault.security.JwtFilter;
import com.streamvault.security.JwtUtil;
import com.streamvault.service.AuthService;
import com.streamvault.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(value = VideoController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class))
@Import(WebConfig.class) // registers the ResourceRegion converter used by /stream
@DisplayName("VideoController Tests")
class VideoControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private VideoService videoService;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private JwtUtil jwtUtil;

        private User testUser;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .password("encoded")
                                .build();
        }

        @Nested
        @DisplayName("POST /api/videos/upload")
        class UploadEndpoint {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should upload video successfully")
                void upload_success() throws Exception {
                        when(authService.getUserByUsername("testuser")).thenReturn(testUser);

                        VideoResponse response = VideoResponse.builder()
                                        .id(1L)
                                        .title("Test Video")
                                        .contentType("video/mp4")
                                        .size(1024L)
                                        .uploadDate(LocalDateTime.of(2026, 3, 15, 10, 0))
                                        .build();

                        when(videoService.uploadVideo(any(), eq("Test Video"), eq(testUser)))
                                        .thenReturn(response);

                        MockMultipartFile file = new MockMultipartFile(
                                        "file", "test.mp4", "video/mp4", "fake video".getBytes());

                        mockMvc.perform(multipart("/api/videos/upload")
                                        .file(file)
                                        .param("title", "Test Video")
                                        .with(csrf()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(1))
                                        .andExpect(jsonPath("$.title").value("Test Video"))
                                        .andExpect(jsonPath("$.contentType").value("video/mp4"));
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should return 400 when upload fails")
                void upload_failure() throws Exception {
                        when(authService.getUserByUsername("testuser")).thenReturn(testUser);
                        when(videoService.uploadVideo(any(), any(), any()))
                                        .thenThrow(new BadRequestException("Unsupported file type"));

                        MockMultipartFile file = new MockMultipartFile(
                                        "file", "test.mp4", "video/mp4", "fake video".getBytes());

                        mockMvc.perform(multipart("/api/videos/upload")
                                        .file(file)
                                        .param("title", "Test Video")
                                        .with(csrf()))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.error").value("Unsupported file type"));
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should return a generic 500 without leaking internal error details")
                void upload_unexpectedError() throws Exception {
                        when(authService.getUserByUsername("testuser")).thenReturn(testUser);
                        when(videoService.uploadVideo(any(), any(), any()))
                                        .thenThrow(new RuntimeException("ERROR: duplicate key value violates constraint"));

                        MockMultipartFile file = new MockMultipartFile(
                                        "file", "test.mp4", "video/mp4", "fake video".getBytes());

                        mockMvc.perform(multipart("/api/videos/upload")
                                        .file(file)
                                        .param("title", "Test Video")
                                        .with(csrf()))
                                        .andExpect(status().isInternalServerError())
                                        .andExpect(jsonPath("$.error").value("Internal server error"));
                }

                @Test
                @DisplayName("Should return 401 when not authenticated")
                void upload_unauthenticated() throws Exception {
                        MockMultipartFile file = new MockMultipartFile(
                                        "file", "test.mp4", "video/mp4", "fake video".getBytes());

                        mockMvc.perform(multipart("/api/videos/upload")
                                        .file(file)
                                        .param("title", "Test Video")
                                        .with(csrf()))
                                        .andExpect(status().isUnauthorized());
                }
        }

        @Nested
        @DisplayName("GET /api/videos")
        class ListVideosEndpoint {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should return list of user videos")
                void listVideos_success() throws Exception {
                        when(authService.getUserByUsername("testuser")).thenReturn(testUser);

                        List<VideoResponse> videos = List.of(
                                        VideoResponse.builder()
                                                        .id(1L).title("Video 1").contentType("video/mp4")
                                                        .size(1024L).uploadDate(LocalDateTime.now()).build(),
                                        VideoResponse.builder()
                                                        .id(2L).title("Video 2").contentType("video/webm")
                                                        .size(2048L).uploadDate(LocalDateTime.now()).build());

                        when(videoService.getUserVideos(1L)).thenReturn(videos);

                        mockMvc.perform(get("/api/videos"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.length()").value(2))
                                        .andExpect(jsonPath("$[0].title").value("Video 1"))
                                        .andExpect(jsonPath("$[1].title").value("Video 2"));
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should return empty list when no videos")
                void listVideos_empty() throws Exception {
                        when(authService.getUserByUsername("testuser")).thenReturn(testUser);
                        when(videoService.getUserVideos(1L)).thenReturn(List.of());

                        mockMvc.perform(get("/api/videos"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.length()").value(0));
                }
        }

        @Nested
        @DisplayName("DELETE /api/videos/{id}")
        class DeleteVideoEndpoint {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should delete video successfully")
                void delete_success() throws Exception {
                        when(authService.getUserByUsername("testuser")).thenReturn(testUser);
                        doNothing().when(videoService).deleteVideo(1L, 1L);

                        mockMvc.perform(delete("/api/videos/1").with(csrf()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Video deleted successfully"));

                        verify(videoService).deleteVideo(1L, 1L);
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should return 404 when deleting someone else's video")
                void delete_unauthorized() throws Exception {
                        when(authService.getUserByUsername("testuser")).thenReturn(testUser);
                        doThrow(new NotFoundException("Video not found"))
                                        .when(videoService).deleteVideo(1L, 1L);

                        mockMvc.perform(delete("/api/videos/1").with(csrf()))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.error").value("Video not found"));
                }
        }

        @Nested
        @DisplayName("GET /api/videos/{id}")
        class GetVideoEndpoint {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should return a single video with metadata")
                void getVideo_success() throws Exception {
                        when(authService.getUserByUsername("testuser")).thenReturn(testUser);

                        VideoResponse response = VideoResponse.builder()
                                        .id(1L).title("My Video").contentType("video/mp4")
                                        .size(1024L).uploadDate(LocalDateTime.now())
                                        .durationSeconds(123.4).width(1920).height(1080)
                                        .status(VideoStatus.READY).hasThumbnail(true)
                                        .build();

                        when(videoService.getUserVideo(1L, 1L)).thenReturn(response);

                        mockMvc.perform(get("/api/videos/1"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(1))
                                        .andExpect(jsonPath("$.durationSeconds").value(123.4))
                                        .andExpect(jsonPath("$.status").value("READY"))
                                        .andExpect(jsonPath("$.hasThumbnail").value(true));
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should return 404 when video belongs to another user")
                void getVideo_unauthorized() throws Exception {
                        when(authService.getUserByUsername("testuser")).thenReturn(testUser);
                        when(videoService.getUserVideo(1L, 1L))
                                        .thenThrow(new NotFoundException("Video not found"));

                        mockMvc.perform(get("/api/videos/1"))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.error").value("Video not found"));
                }
        }

        @Nested
        @DisplayName("PUT /api/videos/{id}")
        class UpdateVideoEndpoint {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should update title, description and tags")
                void update_success() throws Exception {
                        when(authService.getUserByUsername("testuser")).thenReturn(testUser);

                        VideoResponse response = VideoResponse.builder()
                                        .id(1L).title("New Title").description("New description")
                                        .tags(List.of("tag1", "tag2")).status(VideoStatus.READY)
                                        .build();

                        when(videoService.updateVideo(eq(1L), eq(1L), eq("New Title"),
                                        eq("New description"), eq(List.of("tag1", "tag2"))))
                                        .thenReturn(response);

                        mockMvc.perform(put("/api/videos/1")
                                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"title": "New Title", "description": "New description",
                                                         "tags": ["tag1", "tag2"]}
                                                        """)
                                        .with(csrf()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.title").value("New Title"))
                                        .andExpect(jsonPath("$.tags.length()").value(2));
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should return 404 when updating someone else's video")
                void update_unauthorized() throws Exception {
                        when(authService.getUserByUsername("testuser")).thenReturn(testUser);
                        when(videoService.updateVideo(any(), any(), any(), any(), any()))
                                        .thenThrow(new NotFoundException("Video not found"));

                        mockMvc.perform(put("/api/videos/1")
                                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                        .content("{\"title\": \"x\"}")
                                        .with(csrf()))
                                        .andExpect(status().isNotFound())
                                        .andExpect(jsonPath("$.error").value("Video not found"));
                }

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should return 400 for an oversized title or too many tags")
                void update_validation() throws Exception {
                        mockMvc.perform(put("/api/videos/1")
                                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                        .content("{\"title\": \"" + "x".repeat(256) + "\"}")
                                        .with(csrf()))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.error").exists());

                        String tags = "[" + "\"t\",".repeat(21) + "\"t\"]";
                        mockMvc.perform(put("/api/videos/1")
                                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                        .content("{\"tags\": " + tags + "}")
                                        .with(csrf()))
                                        .andExpect(status().isBadRequest());

                        verify(videoService, never()).updateVideo(any(), any(), any(), any(), any());
                }
        }

        @Nested
        @DisplayName("GET /api/videos/{id}/thumbnail")
        class ThumbnailEndpoint {

                @Test
                @WithMockUser
                @DisplayName("Should serve thumbnail as JPEG with cache header")
                void thumbnail_success() throws Exception {
                        ByteArrayResource resource = new ByteArrayResource("fake jpeg".getBytes());
                        when(videoService.isValidMediaSignature(1L, "thumbnail", 123L, "good")).thenReturn(true);
                        when(videoService.getThumbnailResource(1L)).thenReturn(resource);

                        mockMvc.perform(get("/api/videos/1/thumbnail?exp=123&sig=good"))
                                        .andExpect(status().isOk())
                                        .andExpect(header().string("Content-Type", "image/jpeg"))
                                        .andExpect(header().string("Cache-Control", "max-age=3600, private"));
                }

                @Test
                @WithMockUser
                @DisplayName("Should return 404 when thumbnail is not available")
                void thumbnail_notFound() throws Exception {
                        when(videoService.isValidMediaSignature(1L, "thumbnail", 123L, "good")).thenReturn(true);
                        when(videoService.getThumbnailResource(1L))
                                        .thenThrow(new NotFoundException("Thumbnail not available"));

                        mockMvc.perform(get("/api/videos/1/thumbnail?exp=123&sig=good"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @WithMockUser
                @DisplayName("Should return 404 without a valid signature, even for an existing thumbnail")
                void thumbnail_unsigned() throws Exception {
                        mockMvc.perform(get("/api/videos/1/thumbnail"))
                                        .andExpect(status().isNotFound());
                        mockMvc.perform(get("/api/videos/1/thumbnail?exp=123&sig=forged"))
                                        .andExpect(status().isNotFound());

                        verify(videoService, never()).getThumbnailResource(any());
                }
        }

        @Nested
        @DisplayName("GET /api/videos/{id}/captions.vtt")
        class CaptionsEndpoint {

                @Test
                @WithMockUser
                @DisplayName("Should serve captions as WebVTT with a valid signature")
                void captions_success() throws Exception {
                        when(videoService.isValidMediaSignature(1L, "captions", 123L, "good")).thenReturn(true);
                        when(videoService.getCaptionsResource(1L))
                                        .thenReturn(new ByteArrayResource("WEBVTT\n\n".getBytes()));

                        mockMvc.perform(get("/api/videos/1/captions.vtt?exp=123&sig=good"))
                                        .andExpect(status().isOk())
                                        .andExpect(header().string("Content-Type", "text/vtt;charset=UTF-8"));
                }

                @Test
                @WithMockUser
                @DisplayName("Should return 404 without a valid signature")
                void captions_unsigned() throws Exception {
                        mockMvc.perform(get("/api/videos/1/captions.vtt"))
                                        .andExpect(status().isNotFound());

                        verify(videoService, never()).getCaptionsResource(any());
                }
        }

        @Nested
        @DisplayName("GET /api/videos/stream/{id}")
        class StreamVideoEndpoint {

                @Test
                @WithMockUser
                @DisplayName("Should stream full video without Range header")
                void stream_fullVideo() throws Exception {
                        byte[] content = "fake video content bytes".getBytes();
                        ByteArrayResource resource = new ByteArrayResource(content);

                        Video video = Video.builder()
                                        .id(1L).title("Stream Test").filename("stream.mp4")
                                        .contentType("video/mp4").size((long) content.length)
                                        .uploadDate(LocalDateTime.now()).user(testUser).build();

                        when(videoService.isValidMediaSignature(1L, "stream", 123L, "good")).thenReturn(true);
                        when(videoService.getVideoById(1L)).thenReturn(video);
                        when(videoService.getVideoResource(1L)).thenReturn(resource);

                        mockMvc.perform(get("/api/videos/stream/1?exp=123&sig=good"))
                                        .andExpect(status().isOk())
                                        .andExpect(header().string("Content-Type", "video/mp4"))
                                        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                                        .andExpect(header().string("Content-Disposition", "inline"));
                }

                @Test
                @WithMockUser
                @DisplayName("Should serve a legacy upload's stored text/html content type as octet-stream")
                void stream_ignoresStoredContentType() throws Exception {
                        Video video = Video.builder()
                                        .id(1L).title("Legacy").filename("legacy.html")
                                        .contentType("text/html").size(4L)
                                        .uploadDate(LocalDateTime.now()).user(testUser).build();

                        when(videoService.isValidMediaSignature(1L, "stream", 123L, "good")).thenReturn(true);
                        when(videoService.getVideoById(1L)).thenReturn(video);
                        when(videoService.getVideoResource(1L)).thenReturn(new ByteArrayResource("<h1>".getBytes()));

                        mockMvc.perform(get("/api/videos/stream/1?exp=123&sig=good"))
                                        .andExpect(status().isOk())
                                        .andExpect(header().string("Content-Type", "application/octet-stream"));
                }

                @Test
                @WithMockUser
                @DisplayName("Should return 404 for unsigned or forged stream requests, with or without Range")
                void stream_unsigned() throws Exception {
                        mockMvc.perform(get("/api/videos/stream/1"))
                                        .andExpect(status().isNotFound());
                        mockMvc.perform(get("/api/videos/stream/1?exp=123&sig=forged"))
                                        .andExpect(status().isNotFound());
                        mockMvc.perform(get("/api/videos/stream/1").header("Range", "bytes=0-10"))
                                        .andExpect(status().isNotFound());

                        verify(videoService, never()).getVideoResource(any());
                }

                @Test
                @WithMockUser
                @DisplayName("Should return 404 when a signed video no longer exists")
                void stream_missingVideo() throws Exception {
                        when(videoService.isValidMediaSignature(1L, "stream", 123L, "good")).thenReturn(true);
                        when(videoService.getVideoById(1L)).thenThrow(new NotFoundException("Video not found"));

                        mockMvc.perform(get("/api/videos/stream/1?exp=123&sig=good").header("Range", "bytes=0-10"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @WithMockUser
                @DisplayName("Should stream partial content with Range header")
                void stream_partialContent() throws Exception {
                        byte[] content = "fake video content bytes that is longer for range testing purposes"
                                        .getBytes();
                        ByteArrayResource resource = new ByteArrayResource(content);

                        Video video = Video.builder()
                                        .id(1L).title("Range Test").filename("range.mp4")
                                        .contentType("video/mp4").size((long) content.length)
                                        .uploadDate(LocalDateTime.now()).user(testUser).build();

                        when(videoService.isValidMediaSignature(1L, "stream", 123L, "good")).thenReturn(true);
                        when(videoService.getVideoById(1L)).thenReturn(video);
                        when(videoService.getVideoResource(1L)).thenReturn(resource);

                        mockMvc.perform(get("/api/videos/stream/1?exp=123&sig=good")
                                        .header("Range", "bytes=0-10"))
                                        .andExpect(status().isPartialContent())
                                        .andExpect(header().exists("Content-Range"))
                                        .andExpect(header().string("Accept-Ranges", "bytes"));
                }
        }
}
