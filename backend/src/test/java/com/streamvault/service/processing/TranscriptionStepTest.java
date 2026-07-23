package com.streamvault.service.processing;

import com.streamvault.model.Video;
import com.streamvault.service.transcription.Transcript;
import com.streamvault.service.transcription.TranscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranscriptionStep Tests")
class TranscriptionStepTest {

    @Mock
    private TranscriptionService transcriptionService;

    @TempDir
    Path tempDir;

    private TranscriptionStep step;
    private ProcessingContext ctx;
    private Path wavFile;

    @BeforeEach
    void setUp() throws Exception {
        step = new TranscriptionStep(transcriptionService);
        Video video = Video.builder().id(1L).title("Test").filename("abc.mp4").build();
        ctx = new ProcessingContext(video, tempDir.resolve("abc.mp4"), tempDir);
        wavFile = tempDir.resolve("abc_audio.wav");
        Files.write(wavFile, new byte[] { 1 });
        ctx.setAudioPath(wavFile);
    }

    @Test
    @DisplayName("Should store transcript, write VTT, and delete the temp WAV")
    void process_success() throws Exception {
        Transcript transcript = new Transcript(List.of(
                new Transcript.Segment(0.0, 2.0, " Hello world.")));
        when(transcriptionService.isAvailable()).thenReturn(true);
        when(transcriptionService.transcribe(wavFile)).thenReturn(Optional.of(transcript));

        step.process(ctx);

        assertEquals("abc.vtt", ctx.getVideo().getCaptionsFilename());
        assertEquals("Hello world.", ctx.getVideo().getTranscriptText());
        assertSame(transcript, ctx.getTranscript());
        String vtt = Files.readString(tempDir.resolve("abc.vtt"));
        assertTrue(vtt.startsWith("WEBVTT"));
        assertFalse(Files.exists(wavFile), "temp WAV should be deleted");
    }

    @Test
    @DisplayName("Should delete the temp WAV even when transcription fails")
    void process_failureStillCleansUp() throws Exception {
        when(transcriptionService.isAvailable()).thenReturn(true);
        when(transcriptionService.transcribe(wavFile)).thenReturn(Optional.empty());

        step.process(ctx);

        assertNull(ctx.getVideo().getCaptionsFilename());
        assertFalse(Files.exists(wavFile), "temp WAV should be deleted");
    }

    @Test
    @DisplayName("Should skip when no audio was extracted")
    void process_noAudio() throws Exception {
        ctx.setAudioPath(null);

        step.process(ctx);

        verify(transcriptionService, never()).transcribe(org.mockito.ArgumentMatchers.any());
        assertNull(ctx.getVideo().getCaptionsFilename());
    }

    @Test
    @DisplayName("Should be an optional step")
    void isOptional() {
        assertFalse(step.required());
        assertEquals("transcription", step.name());
    }
}
