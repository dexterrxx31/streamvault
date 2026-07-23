package com.streamvault.service.ai;

import com.streamvault.service.transcription.Transcript;

import java.util.Optional;

/**
 * Generates a summary and chapter markers from a timestamped transcript.
 * Implementations must degrade gracefully: return {@link Optional#empty()}
 * when unavailable or on failure.
 */
public interface ChapterSuggestionService {

    Optional<ChapterSuggestions> suggestChapters(Transcript transcript, String title);
}
