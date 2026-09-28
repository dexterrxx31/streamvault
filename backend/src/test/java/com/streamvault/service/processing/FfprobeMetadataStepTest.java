package com.streamvault.service.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamvault.model.Video;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FfprobeMetadataStep Tests")
class FfprobeMetadataStepTest {

    private static final String FFPROBE_JSON = """
            {
              "streams": [
                {"codec_type": "audio", "sample_rate": "44100"},
                {"codec_type": "video", "width": 1920, "height": 1080}
              ],
              "format": {"duration": "123.456", "format_name": "mov,mp4,m4a,3gp,3g2,mj2"}
            }
            """;

    @Mock
    private CommandRunner commandRunner;

    @TempDir
    Path tempDir;

    private FfprobeMetadataStep step;
    private ProcessingContext ctx;

    @BeforeEach
    void setUp() {
        step = new FfprobeMetadataStep(commandRunner, new ObjectMapper());
        ReflectionTestUtils.setField(step, "ffprobePath", "ffprobe");

        Video video = Video.builder().id(1L).title("Test").filename("v.mp4").build();
        ctx = new ProcessingContext(video, tempDir.resolve("v.mp4"), tempDir);
    }

    @Test
    @DisplayName("Should extract duration and resolution from ffprobe JSON")
    void process_success() throws Exception {
        when(commandRunner.run(anyList(), any(Duration.class)))
                .thenReturn(new CommandRunner.CommandResult(0, FFPROBE_JSON, ""));

        step.process(ctx);

        assertEquals(123.456, ctx.getVideo().getDurationSeconds());
        assertEquals(1920, ctx.getVideo().getWidth());
        assertEquals(1080, ctx.getVideo().getHeight());
    }

    @Test
    @DisplayName("Should throw when ffprobe exits non-zero")
    void process_ffprobeFails() throws Exception {
        when(commandRunner.run(anyList(), any(Duration.class)))
                .thenReturn(new CommandRunner.CommandResult(1, "", "boom"));

        assertThrows(RuntimeException.class, () -> step.process(ctx));
    }

    @Test
    @DisplayName("Should reject containers outside the allowlist (e.g. HLS playlists, concat scripts)")
    void process_rejectsDisallowedFormats() throws Exception {
        for (String format : List.of("hls", "concat", "image2", "")) {
            when(commandRunner.run(anyList(), any(Duration.class)))
                    .thenReturn(new CommandRunner.CommandResult(0,
                            "{\"format\": {\"duration\": \"5\", \"format_name\": \"" + format + "\"}}", ""));

            assertThrows(RuntimeException.class, () -> step.process(ctx), format);
        }
    }

    @Test
    @DisplayName("Should accept matroska/webm")
    void process_acceptsWebm() throws Exception {
        when(commandRunner.run(anyList(), any(Duration.class)))
                .thenReturn(new CommandRunner.CommandResult(0,
                        "{\"format\": {\"duration\": \"5\", \"format_name\": \"matroska,webm\"}}", ""));

        step.process(ctx);

        assertEquals(5.0, ctx.getVideo().getDurationSeconds());
    }

    @Test
    @DisplayName("Should be a required step")
    void isRequired() {
        assertTrue(step.required());
        assertEquals("ffprobe-metadata", step.name());
    }

    @Test
    @DisplayName("Should pass video path to ffprobe command")
    void process_commandContainsVideoPath() throws Exception {
        when(commandRunner.run(anyList(), any(Duration.class)))
                .thenReturn(new CommandRunner.CommandResult(0, FFPROBE_JSON, ""));

        step.process(ctx);

        org.mockito.ArgumentCaptor<List<String>> captor = org.mockito.ArgumentCaptor.captor();
        org.mockito.Mockito.verify(commandRunner).run(captor.capture(), any(Duration.class));
        List<String> command = captor.getValue();
        assertEquals("ffprobe", command.get(0));
        assertTrue(command.contains(ctx.getVideoPath().toString()));
        int whitelist = command.indexOf("-protocol_whitelist");
        assertTrue(whitelist >= 0, "ffprobe must restrict protocols");
        assertEquals("file", command.get(whitelist + 1));
    }
}
