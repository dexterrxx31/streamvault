package com.streamvault.service.processing;

import com.streamvault.model.Video;
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
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ThumbnailStep Tests")
class ThumbnailStepTest {

    @Mock
    private CommandRunner commandRunner;

    @TempDir
    Path tempDir;

    private ThumbnailStep step;
    private ProcessingContext ctx;

    @BeforeEach
    void setUp() {
        step = new ThumbnailStep(commandRunner);
        ReflectionTestUtils.setField(step, "ffmpegPath", "ffmpeg");

        Video video = Video.builder()
                .id(1L).title("Test").filename("abc123.mp4")
                .durationSeconds(100.0)
                .build();
        ctx = new ProcessingContext(video, tempDir.resolve("abc123.mp4"), tempDir);
    }

    /** Simulates ffmpeg creating the output file (last command argument). */
    private void mockFfmpegCreatesOutput() throws Exception {
        when(commandRunner.run(anyList(), any(Duration.class))).thenAnswer(invocation -> {
            List<String> command = invocation.getArgument(0);
            Files.write(Paths.get(command.get(command.size() - 1)), new byte[] { 1 });
            return new CommandRunner.CommandResult(0, "", "");
        });
    }

    @Test
    @DisplayName("Should generate poster thumbnail and 4 frames")
    void process_success() throws Exception {
        mockFfmpegCreatesOutput();

        step.process(ctx);

        assertEquals("abc123_thumb.jpg", ctx.getVideo().getThumbnailFilename());
        assertTrue(Files.exists(tempDir.resolve("abc123_thumb.jpg")));
        assertEquals(4, ctx.getFrames().size());
        assertTrue(Files.exists(tempDir.resolve("abc123_frame_1.jpg")));
        assertTrue(Files.exists(tempDir.resolve("abc123_frame_4.jpg")));
    }

    @Test
    @DisplayName("Should throw when poster thumbnail generation fails")
    void process_posterFails() throws Exception {
        when(commandRunner.run(anyList(), any(Duration.class)))
                .thenReturn(new CommandRunner.CommandResult(1, "", "ffmpeg error"));

        assertThrows(RuntimeException.class, () -> step.process(ctx));
        assertNull(ctx.getVideo().getThumbnailFilename());
    }

    @Test
    @DisplayName("Should be a required step")
    void isRequired() {
        assertTrue(step.required());
        assertEquals("thumbnail", step.name());
    }
}
