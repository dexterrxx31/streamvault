package com.streamvault.service.processing;

import com.streamvault.service.ai.MetadataSuggestionService;
import com.streamvault.service.ai.SuggestedMetadata;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Optional step: asks Claude for title/description/tag suggestions based on
 * the frames extracted by {@link ThumbnailStep}. The video reaches READY even
 * when this step is skipped or fails.
 */
@Component
@Order(30)
public class AiMetadataStep implements ProcessingStep {

    private final MetadataSuggestionService suggestionService;

    public AiMetadataStep(MetadataSuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @Override
    public String name() {
        return "ai-metadata";
    }

    @Override
    public boolean required() {
        return false;
    }

    @Override
    public void process(ProcessingContext ctx) {
        Optional<SuggestedMetadata> suggestion = suggestionService.suggestMetadata(
                ctx.getFrames(), ctx.getVideo().getTitle());

        suggestion.ifPresent(s -> {
            ctx.getVideo().setAiTitle(s.title());
            ctx.getVideo().setAiDescription(s.description());
            if (s.tags() != null && !s.tags().isEmpty()) {
                ctx.getVideo().setAiTags(String.join(",", s.tags()));
            }
        });
    }
}
