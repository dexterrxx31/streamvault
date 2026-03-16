package com.streamvault.service;

import com.streamvault.dto.VideoResponse;
import com.streamvault.model.User;
import com.streamvault.model.Video;
import com.streamvault.repository.VideoRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("VideoService Tests")
class VideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @InjectMocks
    private VideoService videoService;

    @TempDir
    Path tempDir;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .build();

        // Set the upload path via reflection since @Value is not processed in unit
        // tests
        ReflectionTestUtils.setField(videoService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(videoService, "uploadPath", tempDir);
    }

    @Nested
    @DisplayName("Upload Video Tests")
    class UploadVideoTests {

        @Test
        @DisplayName("Should upload video successfully")
        void uploadVideo_success() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("test_video.mp4");
            when(file.getContentType()).thenReturn("video/mp4");
            when(file.getSize()).thenReturn(1024L);
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("fake video data".getBytes()));

            Video savedVideo = Video.builder()
                    .id(1L)
                    .title("My Video")
                    .filename("stored.mp4")
                    .contentType("video/mp4")
                    .size(1024L)
                    .uploadDate(LocalDateTime.now())
                    .user(testUser)
                    .build();

            when(videoRepository.save(any(Video.class))).thenReturn(savedVideo);

            VideoResponse response = videoService.uploadVideo(file, "My Video", testUser);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("My Video", response.getTitle());
            assertEquals("video/mp4", response.getContentType());
            assertEquals(1024L, response.getSize());

            verify(videoRepository).save(any(Video.class));
        }

        @Test
        @DisplayName("Should upload video with no extension")
        void uploadVideo_noExtension() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("videofile");
            when(file.getContentType()).thenReturn("video/mp4");
            when(file.getSize()).thenReturn(512L);
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));

            Video savedVideo = Video.builder()
                    .id(2L)
                    .title("No Extension Video")
                    .filename("stored")
                    .contentType("video/mp4")
                    .size(512L)
                    .uploadDate(LocalDateTime.now())
                    .user(testUser)
                    .build();

            when(videoRepository.save(any(Video.class))).thenReturn(savedVideo);

            VideoResponse response = videoService.uploadVideo(file, "No Extension Video", testUser);

            assertNotNull(response);
            assertEquals("No Extension Video", response.getTitle());
            verify(videoRepository).save(any(Video.class));
        }

        @Test
        @DisplayName("Should throw when file IO fails")
        void uploadVideo_ioFailure() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("test.mp4");
            when(file.getInputStream()).thenThrow(new IOException("Disk full"));

            assertThrows(RuntimeException.class,
                    () -> videoService.uploadVideo(file, "Fail Video", testUser));

            verify(videoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get User Videos Tests")
    class GetUserVideosTests {

        @Test
        @DisplayName("Should return list of user videos")
        void getUserVideos_success() {
            Video video1 = Video.builder()
                    .id(1L).title("Video 1").filename("v1.mp4")
                    .contentType("video/mp4").size(1024L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            Video video2 = Video.builder()
                    .id(2L).title("Video 2").filename("v2.mp4")
                    .contentType("video/webm").size(2048L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findByUserIdOrderByUploadDateDesc(1L))
                    .thenReturn(List.of(video1, video2));

            List<VideoResponse> videos = videoService.getUserVideos(1L);

            assertEquals(2, videos.size());
            assertEquals("Video 1", videos.get(0).getTitle());
            assertEquals("Video 2", videos.get(1).getTitle());
        }

        @Test
        @DisplayName("Should return empty list when no videos")
        void getUserVideos_empty() {
            when(videoRepository.findByUserIdOrderByUploadDateDesc(1L))
                    .thenReturn(List.of());

            List<VideoResponse> videos = videoService.getUserVideos(1L);

            assertTrue(videos.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get Video By Id Tests")
    class GetVideoByIdTests {

        @Test
        @DisplayName("Should return video when found")
        void getVideoById_found() {
            Video video = Video.builder()
                    .id(1L).title("Found Video").filename("v.mp4")
                    .contentType("video/mp4").size(1024L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

            Video result = videoService.getVideoById(1L);

            assertNotNull(result);
            assertEquals("Found Video", result.getTitle());
        }

        @Test
        @DisplayName("Should throw when video not found")
        void getVideoById_notFound() {
            when(videoRepository.findById(99L)).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> videoService.getVideoById(99L));
            assertEquals("Video not found", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Get Video Resource Tests")
    class GetVideoResourceTests {

        @Test
        @DisplayName("Should return resource for existing video file")
        void getVideoResource_success() throws IOException {
            // Create actual file in temp dir
            Path videoFile = tempDir.resolve("test-video.mp4");
            Files.write(videoFile, "video content".getBytes());

            Video video = Video.builder()
                    .id(1L).title("Test").filename("test-video.mp4")
                    .contentType("video/mp4").size(100L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

            Resource resource = videoService.getVideoResource(1L);

            assertNotNull(resource);
            assertTrue(resource.exists());
        }

        @Test
        @DisplayName("Should throw when video file does not exist on disk")
        void getVideoResource_fileMissing() {
            Video video = Video.builder()
                    .id(1L).title("Missing").filename("nonexistent.mp4")
                    .contentType("video/mp4").size(100L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

            assertThrows(RuntimeException.class,
                    () -> videoService.getVideoResource(1L));
        }
    }

    @Nested
    @DisplayName("Delete Video Tests")
    class DeleteVideoTests {

        @Test
        @DisplayName("Should delete video owned by user")
        void deleteVideo_success() throws IOException {
            // Create actual file
            Path videoFile = tempDir.resolve("to-delete.mp4");
            Files.write(videoFile, "content".getBytes());

            Video video = Video.builder()
                    .id(1L).title("Delete Me").filename("to-delete.mp4")
                    .contentType("video/mp4").size(100L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

            videoService.deleteVideo(1L, 1L);

            verify(videoRepository).delete(video);
            assertFalse(Files.exists(videoFile));
        }

        @Test
        @DisplayName("Should throw when deleting video owned by different user")
        void deleteVideo_unauthorized() {
            Video video = Video.builder()
                    .id(1L).title("Not Yours").filename("v.mp4")
                    .contentType("video/mp4").size(100L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> videoService.deleteVideo(1L, 999L));
            assertEquals("Not authorized to delete this video", exception.getMessage());

            verify(videoRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw when video not found for deletion")
        void deleteVideo_videoNotFound() {
            when(videoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> videoService.deleteVideo(99L, 1L));

            verify(videoRepository, never()).delete(any());
        }
    }
}
