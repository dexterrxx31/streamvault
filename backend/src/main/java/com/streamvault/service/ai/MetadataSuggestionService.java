package com.streamvault.service.ai;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Suggests video metadata from sampled frames. Implementations must degrade
 * gracefully: return {@link Optional#empty()} when unavailable or on failure.
 */
public interface MetadataSuggestionService {

    Optional<SuggestedMetadata> suggestMetadata(List<Path> frames, String currentTitle);
}
