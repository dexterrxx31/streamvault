package com.streamvault.service.processing;

import com.streamvault.model.ChapterMarker;
import com.streamvault.service.ai.ChapterSuggestionService;
import com.streamvault.service.ai.ChapterSuggestions;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Optional step: asks Claude for a summary and chapter markers from the
 * transcript. Skips when there is no transcript or no AI credentials.
 */
@Component
@Order(60)
public class ChapterGenerationStep implements ProcessingStep {

    private final ChapterSuggestionService chapterSuggestionService;

    public ChapterGenerationStep(ChapterSuggestionService chapterSuggestionService) {
        this.chapterSuggestionService = chapterSuggestionService;
    }

    @Override
    public String name() {
        return "chapter-generation";
    }

    @Override
    public boolean required() {
        return false;
    }

    @Override
    public void process(ProcessingContext ctx) {
        if (ctx.getTranscript() == null) {
            return;
        }
        Optional<ChapterSuggestions> suggestion = chapterSuggestionService.suggestChapters(
                ctx.getTranscript(), ctx.getVideo().getTitle());

        suggestion.ifPresent(s -> {
            ctx.getVideo().setSummary(s.summary());
            if (s.chapters() != null) {
                List<ChapterMarker> markers = s.chapters().stream()
                        .map(c -> new ChapterMarker(c.startSeconds(), c.title()))
                        .toList();
                ctx.getVideo().setChapters(markers);
            }
        });
    }
}
