package com.streamvault.service;

import com.streamvault.dto.VideoResponse;
import com.streamvault.exception.BadRequestException;
import com.streamvault.exception.NotFoundException;
import com.streamvault.exception.ServiceUnavailableException;
import com.streamvault.model.User;
import com.streamvault.model.Video;
import com.streamvault.repository.VideoRepository;
import com.streamvault.security.MediaUrlSigner;
import com.streamvault.service.processing.VideoUploadedEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskRejectedException;
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private MediaUrlSigner mediaUrlSigner = new MediaUrlSigner("test-media-secret-that-is-at-least-32-bytes", 3600);

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
            verify(eventPublisher).publishEvent(any(VideoUploadedEvent.class));
        }

        @Test
        @DisplayName("Should reject a file with no extension")
        void uploadVideo_noExtension() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("videofile");

            assertThrows(BadRequestException.class,
                    () -> videoService.uploadVideo(file, "No Extension Video", testUser));
            verify(videoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject non-video files even when the client claims a video content type")
        void uploadVideo_rejectsHtml() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("evil.html");

            assertThrows(BadRequestException.class,
                    () -> videoService.uploadVideo(file, "Evil", testUser));
            verify(videoRepository, never()).save(any());
            assertEquals(0, tempDir.toFile().list().length);
        }

        @Test
        @DisplayName("Should store a server-derived content type, ignoring the client's")
        void uploadVideo_ignoresClientContentType() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("clip.WEBM");
            when(file.getSize()).thenReturn(10L);
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
            when(videoRepository.save(any(Video.class))).thenAnswer(inv -> {
                Video v = inv.getArgument(0);
                v.setId(5L);
                return v;
            });

            videoService.uploadVideo(file, "Clip", testUser);

            org.mockito.ArgumentCaptor<Video> captor = org.mockito.ArgumentCaptor.forClass(Video.class);
            verify(videoRepository).save(captor.capture());
            assertEquals("video/webm", captor.getValue().getContentType());
            assertTrue(captor.getValue().getFilename().endsWith(".webm"));
        }

        @Test
        @DisplayName("Should reject a blank title")
        void uploadVideo_blankTitle() {
            MultipartFile file = mock(MultipartFile.class);

            assertThrows(BadRequestException.class, () -> videoService.uploadVideo(file, "  ", testUser));
        }

        @Test
        @DisplayName("Should undo the upload and throw 503 when the processing queue is full")
        void uploadVideo_queueFull() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("test.mp4");
            when(file.getSize()).thenReturn(10L);
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
            Video saved = Video.builder().id(7L).title("t").filename("x.mp4").user(testUser).build();
            when(videoRepository.save(any(Video.class))).thenReturn(saved);
            doThrow(new TaskRejectedException("full")).when(eventPublisher).publishEvent(any(VideoUploadedEvent.class));

            assertThrows(ServiceUnavailableException.class,
                    () -> videoService.uploadVideo(file, "t", testUser));

            verify(videoRepository).delete(saved);
            assertEquals(0, tempDir.toFile().list().length);
        }

        @Test
        @DisplayName("Should delete the stored file when the DB save fails")
        void uploadVideo_saveFails() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("test.mp4");
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
            when(videoRepository.save(any(Video.class))).thenThrow(new RuntimeException("db down"));

            assertThrows(RuntimeException.class, () -> videoService.uploadVideo(file, "t", testUser));

            assertEquals(0, tempDir.toFile().list().length);
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
            verify(eventPublisher, never()).publishEvent(any());
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

            // Someone else's video is indistinguishable from a missing one
            RuntimeException exception = assertThrows(NotFoundException.class,
                    () -> videoService.deleteVideo(1L, 999L));
            assertEquals("Video not found", exception.getMessage());

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

        @Test
        @DisplayName("Should delete thumbnail and frame files with the video")
        void deleteVideo_cleansUpDerivedFiles() throws IOException {
            Path videoFile = tempDir.resolve("abc.mp4");
            Path thumbFile = tempDir.resolve("abc_thumb.jpg");
            Path frameFile = tempDir.resolve("abc_frame_1.jpg");
            Path audioFile = tempDir.resolve("abc_audio.wav");
            Files.write(audioFile, "wav".getBytes());
            Files.write(videoFile, "content".getBytes());
            Files.write(thumbFile, "thumb".getBytes());
            Files.write(frameFile, "frame".getBytes());

            Video video = Video.builder()
                    .id(1L).title("Full Cleanup").filename("abc.mp4")
                    .contentType("video/mp4").size(100L)
                    .thumbnailFilename("abc_thumb.jpg")
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

            videoService.deleteVideo(1L, 1L);

            assertFalse(Files.exists(videoFile));
            assertFalse(Files.exists(thumbFile));
            assertFalse(Files.exists(frameFile));
            assertFalse(Files.exists(audioFile));
        }
    }

    @Nested
    @DisplayName("Get User Video Tests")
    class GetUserVideoTests {

        @Test
        @DisplayName("Should return video owned by user")
        void getUserVideo_success() {
            Video video = Video.builder()
                    .id(1L).title("Mine").filename("v.mp4")
                    .contentType("video/mp4").size(100L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

            VideoResponse response = videoService.getUserVideo(1L, 1L);

            assertEquals("Mine", response.getTitle());
            // Owner receives a verifiable signed stream URL; no thumbnail/captions yet
            assertTrue(response.getStreamUrl().startsWith("/api/videos/stream/1?exp="));
            String query = response.getStreamUrl().substring(response.getStreamUrl().indexOf('?') + 1);
            long exp = Long.parseLong(query.split("&")[0].substring(4));
            String sig = query.split("&")[1].substring(4);
            assertTrue(videoService.isValidMediaSignature(1L, VideoService.KIND_STREAM, exp, sig));
            assertFalse(videoService.isValidMediaSignature(2L, VideoService.KIND_STREAM, exp, sig));
            assertFalse(videoService.isValidMediaSignature(1L, VideoService.KIND_CAPTIONS, exp, sig));
            assertNull(response.getThumbnailUrl());
            assertNull(response.getCaptionsUrl());
        }

        @Test
        @DisplayName("Should throw when video belongs to another user")
        void getUserVideo_unauthorized() {
            Video video = Video.builder()
                    .id(1L).title("Not Yours").filename("v.mp4")
                    .contentType("video/mp4").size(100L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

            RuntimeException exception = assertThrows(NotFoundException.class,
                    () -> videoService.getUserVideo(1L, 999L));
            assertEquals("Video not found", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Update Video Tests")
    class UpdateVideoTests {

        @Test
        @DisplayName("Should update title, description and tags")
        void updateVideo_success() {
            Video video = Video.builder()
                    .id(1L).title("Old").filename("v.mp4")
                    .contentType("video/mp4").size(100L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
            when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));

            VideoResponse response = videoService.updateVideo(1L, 1L, "New", "Desc", List.of("a", "b"));

            assertEquals("New", response.getTitle());
            assertEquals("Desc", response.getDescription());
            assertEquals(List.of("a", "b"), response.getTags());
        }

        @Test
        @DisplayName("Should clear AI suggestions on any update")
        void updateVideo_clearsAiSuggestions() {
            Video video = Video.builder()
                    .id(1L).title("Old").filename("v.mp4")
                    .contentType("video/mp4").size(100L)
                    .aiTitle("AI Title").aiDescription("AI Desc").aiTags("x,y")
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
            when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));

            VideoResponse response = videoService.updateVideo(1L, 1L, null, null, null);

            assertNull(response.getAiTitle());
            assertNull(response.getAiDescription());
            assertTrue(response.getAiTags().isEmpty());
            assertEquals("Old", response.getTitle());
        }

        @Test
        @DisplayName("Should keep existing title when new title is blank")
        void updateVideo_blankTitleIgnored() {
            Video video = Video.builder()
                    .id(1L).title("Keep Me").filename("v.mp4")
                    .contentType("video/mp4").size(100L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
            when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));

            VideoResponse response = videoService.updateVideo(1L, 1L, "  ", "Desc", null);

            assertEquals("Keep Me", response.getTitle());
        }

        @Test
        @DisplayName("Should throw when updating video owned by different user")
        void updateVideo_unauthorized() {
            Video video = Video.builder()
                    .id(1L).title("Not Yours").filename("v.mp4")
                    .contentType("video/mp4").size(100L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

            assertThrows(RuntimeException.class,
                    () -> videoService.updateVideo(1L, 999L, "New", null, null));

            verify(videoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Thumbnail Resource Tests")
    class GetThumbnailResourceTests {

        @Test
        @DisplayName("Should return thumbnail resource when it exists")
        void getThumbnailResource_success() throws IOException {
            Path thumbFile = tempDir.resolve("v_thumb.jpg");
            Files.write(thumbFile, "jpeg data".getBytes());

            Video video = Video.builder()
                    .id(1L).title("Test").filename("v.mp4")
                    .contentType("video/mp4").size(100L)
                    .thumbnailFilename("v_thumb.jpg")
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

            Resource resource = videoService.getThumbnailResource(1L);

            assertNotNull(resource);
            assertTrue(resource.exists());
        }

        @Test
        @DisplayName("Should throw when video has no thumbnail yet")
        void getThumbnailResource_noThumbnail() {
            Video video = Video.builder()
                    .id(1L).title("Processing").filename("v.mp4")
                    .contentType("video/mp4").size(100L)
                    .uploadDate(LocalDateTime.now()).user(testUser).build();

            when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> videoService.getThumbnailResource(1L));
            assertEquals("Thumbnail not available", exception.getMessage());
        }
    }
}
