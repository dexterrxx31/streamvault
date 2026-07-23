package com.streamvault.service.processing;

import com.streamvault.model.Video;
import com.streamvault.service.ai.MetadataSuggestionService;
import com.streamvault.service.ai.SuggestedMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiMetadataStep Tests")
class AiMetadataStepTest {

    @Mock
    private MetadataSuggestionService suggestionService;

    @TempDir
    Path tempDir;

    private AiMetadataStep step;
    private ProcessingContext ctx;

    @BeforeEach
    void setUp() {
        step = new AiMetadataStep(suggestionService);
        Video video = Video.builder().id(1L).title("Original Title").filename("v.mp4").build();
        ctx = new ProcessingContext(video, tempDir.resolve("v.mp4"), tempDir);
        ctx.getFrames().add(tempDir.resolve("v_frame_1.jpg"));
    }

    @Test
    @DisplayName("Should write AI suggestions onto the video")
    void process_suggestionPresent() {
        when(suggestionService.suggestMetadata(anyList(), any()))
                .thenReturn(Optional.of(new SuggestedMetadata(
                        "Better Title", "A great description", List.of("tag1", "tag2"))));

        step.process(ctx);

        assertEquals("Better Title", ctx.getVideo().getAiTitle());
        assertEquals("A great description", ctx.getVideo().getAiDescription());
        assertEquals("tag1,tag2", ctx.getVideo().getAiTags());
    }

    @Test
    @DisplayName("Should leave video untouched when no suggestion is returned")
    void process_suggestionEmpty() {
        when(suggestionService.suggestMetadata(anyList(), any()))
                .thenReturn(Optional.empty());

        step.process(ctx);

        assertNull(ctx.getVideo().getAiTitle());
        assertNull(ctx.getVideo().getAiDescription());
        assertNull(ctx.getVideo().getAiTags());
    }

    @Test
    @DisplayName("Should be an optional step")
    void isOptional() {
        assertFalse(step.required());
        assertEquals("ai-metadata", step.name());
    }
}
