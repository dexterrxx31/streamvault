package com.streamvault.service.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * AI-generated summary and chapter markers. Also serves as the
 * structured-output schema for the Claude API call.
 */
public record ChapterSuggestions(
        @JsonPropertyDescription("A 2-4 sentence summary of the video content") String summary,
        @JsonPropertyDescription("Chapter markers dividing the video into sections, in chronological order") List<Chapter> chapters) {

    public record Chapter(
            @JsonPropertyDescription("Chapter start time in seconds from the beginning of the video") double startSeconds,
            @JsonPropertyDescription("A short chapter title (2-6 words)") String title) {
    }
}
