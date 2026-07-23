package com.streamvault.service.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * AI-suggested video metadata. Also serves as the structured-output schema
 * for the Claude API call (field descriptions steer the model).
 */
public record SuggestedMetadata(
        @JsonPropertyDescription("A concise, engaging title for the video") String title,
        @JsonPropertyDescription("A 1-3 sentence description of the video content") String description,
        @JsonPropertyDescription("3 to 6 short lowercase tags categorizing the video") List<String> tags) {
}
