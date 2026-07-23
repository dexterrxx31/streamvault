package com.streamvault.service.processing;

import com.streamvault.model.Video;
import com.streamvault.service.ai.ChapterSuggestionService;
import com.streamvault.service.ai.ChapterSuggestions;
import com.streamvault.service.transcription.Transcript;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChapterGenerationStep Tests")
class ChapterGenerationStepTest {

    @Mock
    private ChapterSuggestionService chapterSuggestionService;

    @TempDir
    Path tempDir;

    private ChapterGenerationStep step;
    private ProcessingContext ctx;

    @BeforeEach
    void setUp() {
        step = new ChapterGenerationStep(chapterSuggestionService);
        Video video = Video.builder().id(1L).title("Test").filename("v.mp4").build();
        ctx = new ProcessingContext(video, tempDir.resolve("v.mp4"), tempDir);
    }

    @Test
    @DisplayName("Should store summary and chapters from the suggestion")
    void process_success() {
        ctx.setTranscript(new Transcript(List.of(new Transcript.Segment(0, 5, "Hello"))));
        when(chapterSuggestionService.suggestChapters(any(), any()))
                .thenReturn(Optional.of(new ChapterSuggestions("A summary.", List.of(
                        new ChapterSuggestions.Chapter(0.0, "Intro"),
                        new ChapterSuggestions.Chapter(12.5, "Main topic")))));

        step.process(ctx);

        assertEquals("A summary.", ctx.getVideo().getSummary());
        assertEquals(2, ctx.getVideo().getChapters().size());
        assertEquals("Intro", ctx.getVideo().getChapters().get(0).getTitle());
        assertEquals(12.5, ctx.getVideo().getChapters().get(1).getStartSeconds());
    }

    @Test
    @DisplayName("Should skip when there is no transcript")
    void process_noTranscript() {
        step.process(ctx);

        verify(chapterSuggestionService, never()).suggestChapters(any(), any());
        assertNull(ctx.getVideo().getSummary());
    }

    @Test
    @DisplayName("Should leave video untouched when no suggestion is returned")
    void process_suggestionEmpty() {
        ctx.setTranscript(new Transcript(List.of(new Transcript.Segment(0, 5, "Hello"))));
        when(chapterSuggestionService.suggestChapters(any(), any())).thenReturn(Optional.empty());

        step.process(ctx);

        assertNull(ctx.getVideo().getSummary());
        assertTrue(ctx.getVideo().getChapters().isEmpty());
    }

    @Test
    @DisplayName("Should be an optional step")
    void isOptional() {
        assertFalse(step.required());
        assertEquals("chapter-generation", step.name());
    }
}
