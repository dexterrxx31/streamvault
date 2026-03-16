package com.streamvault.controller;

import com.streamvault.dto.VideoResponse;
import com.streamvault.model.User;
import com.streamvault.model.Video;
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
                                        .thenThrow(new RuntimeException("Failed to upload video"));

                        MockMultipartFile file = new MockMultipartFile(
                                        "file", "test.mp4", "video/mp4", "fake video".getBytes());

                        mockMvc.perform(multipart("/api/videos/upload")
                                        .file(file)
                                        .param("title", "Test Video")
                                        .with(csrf()))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.error").value("Failed to upload video"));
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
                @DisplayName("Should return 400 when not authorized to delete")
                void delete_unauthorized() throws Exception {
                        when(authService.getUserByUsername("testuser")).thenReturn(testUser);
                        doThrow(new RuntimeException("Not authorized to delete this video"))
                                        .when(videoService).deleteVideo(1L, 1L);

                        mockMvc.perform(delete("/api/videos/1").with(csrf()))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.error").value("Not authorized to delete this video"));
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

                        when(videoService.getVideoById(1L)).thenReturn(video);
                        when(videoService.getVideoResource(1L)).thenReturn(resource);

                        mockMvc.perform(get("/api/videos/stream/1"))
                                        .andExpect(status().isOk())
                                        .andExpect(header().string("Content-Type", "video/mp4"));
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

                        when(videoService.getVideoById(1L)).thenReturn(video);
                        when(videoService.getVideoResource(1L)).thenReturn(resource);

                        mockMvc.perform(get("/api/videos/stream/1")
                                        .header("Range", "bytes=0-10"))
                                        .andExpect(status().isPartialContent())
                                        .andExpect(header().exists("Content-Range"))
                                        .andExpect(header().string("Accept-Ranges", "bytes"));
                }
        }
}
