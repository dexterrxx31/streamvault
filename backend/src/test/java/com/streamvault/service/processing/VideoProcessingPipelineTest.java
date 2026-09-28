package com.streamvault.service.processing;

import com.streamvault.model.Video;
import com.streamvault.model.VideoStatus;
import com.streamvault.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VideoProcessingPipeline Tests")
class VideoProcessingPipelineTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private ProcessingStep requiredStep;

    @Mock
    private ProcessingStep optionalStep;

    @TempDir
    Path tempDir;

    private Video video;

    @BeforeEach
    void setUp() throws Exception {
        video = Video.builder()
                .id(1L).title("Test").filename("v.mp4")
                .status(VideoStatus.PROCESSING)
                .build();
        Files.write(tempDir.resolve("v.mp4"), new byte[] { 1 });
    }

    private VideoProcessingPipeline pipeline(List<ProcessingStep> steps) {
        VideoProcessingPipeline pipeline = new VideoProcessingPipeline(videoRepository, steps);
        ReflectionTestUtils.setField(pipeline, "uploadDir", tempDir.toString());
        return pipeline;
    }

    @Test
    @DisplayName("Should mark video READY when all steps succeed")
    void allStepsSucceed() throws Exception {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        // @Async is only a proxy wrapper — invoking directly runs synchronously
        pipeline(List.of(requiredStep, optionalStep)).onVideoUploaded(new VideoUploadedEvent(1L));

        verify(requiredStep).process(any(ProcessingContext.class));
        verify(optionalStep).process(any(ProcessingContext.class));
        assertEquals(VideoStatus.READY, video.getStatus());
    }

    @Test
    @DisplayName("Should mark video FAILED when a required step fails")
    void requiredStepFails() throws Exception {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
        when(requiredStep.required()).thenReturn(true);
        doThrow(new RuntimeException("ffprobe broke")).when(requiredStep).process(any());

        pipeline(List.of(requiredStep, optionalStep)).onVideoUploaded(new VideoUploadedEvent(1L));

        assertEquals(VideoStatus.FAILED, video.getStatus());
        verify(optionalStep, never()).process(any());
    }

    @Test
    @DisplayName("Should reach READY when only an optional step fails")
    void optionalStepFails() throws Exception {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
        when(optionalStep.required()).thenReturn(false);
        doThrow(new RuntimeException("AI unavailable")).when(optionalStep).process(any());

        pipeline(List.of(optionalStep)).onVideoUploaded(new VideoUploadedEvent(1L));

        assertEquals(VideoStatus.READY, video.getStatus());
    }

    @Test
    @DisplayName("Should skip processing when video was deleted")
    void videoDeleted() throws Exception {
        when(videoRepository.findById(99L)).thenReturn(Optional.empty());

        pipeline(List.of(requiredStep)).onVideoUploaded(new VideoUploadedEvent(99L));

        verify(requiredStep, never()).process(any());
        verify(videoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not overwrite a title the user edited while processing ran")
    void userEditDuringProcessingSurvives() throws Exception {
        // The pipeline's working copy and the DB row are separate objects, as with JPA
        Video dbRow = Video.builder()
                .id(1L).title("Test").filename("v.mp4").status(VideoStatus.PROCESSING).build();
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video), Optional.of(dbRow));
        doAnswer(inv -> {
            dbRow.setTitle("Edited by user");   // user edit lands mid-run
            ((ProcessingContext) inv.getArgument(0)).getVideo().setDurationSeconds(12.5);
            return null;
        }).when(requiredStep).process(any());

        pipeline(List.of(requiredStep)).onVideoUploaded(new VideoUploadedEvent(1L));

        assertEquals("Edited by user", dbRow.getTitle());
        assertEquals(12.5, dbRow.getDurationSeconds());
        assertEquals(VideoStatus.READY, dbRow.getStatus());
    }

    @Test
    @DisplayName("Should not resurrect AI suggestions the user dismissed after the AI step")
    void dismissedSuggestionsStayDismissed() throws Exception {
        Video dbRow = Video.builder()
                .id(1L).title("Test").filename("v.mp4").status(VideoStatus.PROCESSING).build();
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video), Optional.of(dbRow));
        doAnswer(inv -> {
            ((ProcessingContext) inv.getArgument(0)).getVideo().setAiTitle("AI title");
            return null;
        }).when(requiredStep).process(any());
        doAnswer(inv -> {
            dbRow.setAiTitle(null);   // user dismissed between steps
            ((ProcessingContext) inv.getArgument(0)).getVideo().setSummary("summary");
            return null;
        }).when(optionalStep).process(any());

        pipeline(List.of(requiredStep, optionalStep)).onVideoUploaded(new VideoUploadedEvent(1L));

        assertEquals(null, dbRow.getAiTitle());
        assertEquals("summary", dbRow.getSummary());
    }

    @Test
    @DisplayName("Should stop and remove generated files when the video is deleted mid-run")
    void deletedDuringProcessing() throws Exception {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video), Optional.empty());
        doAnswer(inv -> {
            Files.write(tempDir.resolve("v_thumb.jpg"), new byte[] { 1 });
            return null;
        }).when(requiredStep).process(any());

        pipeline(List.of(requiredStep, optionalStep)).onVideoUploaded(new VideoUploadedEvent(1L));

        verify(optionalStep, never()).process(any());
        verify(videoRepository, never()).save(any());
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(tempDir.resolve("v_thumb.jpg")));
    }

    @Test
    @DisplayName("Should mark video FAILED when file is missing on disk")
    void fileMissing() throws Exception {
        video.setFilename("nonexistent.mp4");
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        pipeline(List.of(requiredStep)).onVideoUploaded(new VideoUploadedEvent(1L));

        assertEquals(VideoStatus.FAILED, video.getStatus());
        verify(requiredStep, never()).process(any());
    }
}
