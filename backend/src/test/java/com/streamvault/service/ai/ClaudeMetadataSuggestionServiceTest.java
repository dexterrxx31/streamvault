package com.streamvault.service.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Only the disabled path is tested — the real API is never called from tests.
 * The {@link MetadataSuggestionService} interface is the mocking seam for
 * everything downstream.
 */
@DisplayName("ClaudeMetadataSuggestionService Tests")
class ClaudeMetadataSuggestionServiceTest {

    @Test
    @DisabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    @DisplayName("Should return empty when ANTHROPIC_API_KEY is not set")
    void suggestMetadata_disabledWithoutApiKey() {
        ClaudeMetadataSuggestionService service = new ClaudeMetadataSuggestionService();

        Optional<SuggestedMetadata> result = service.suggestMetadata(
                List.of(Path.of("frame.jpg")), "Some Title");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when no frames are provided")
    void suggestMetadata_noFrames() {
        ClaudeMetadataSuggestionService service = new ClaudeMetadataSuggestionService();

        Optional<SuggestedMetadata> result = service.suggestMetadata(List.of(), "Some Title");

        assertTrue(result.isEmpty());
    }
}
