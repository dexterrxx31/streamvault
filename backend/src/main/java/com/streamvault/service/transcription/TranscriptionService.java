package com.streamvault.service.transcription;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Speech-to-text over a 16kHz mono WAV file. Implementations must degrade
 * gracefully: {@link #isAvailable()} false means the feature is disabled.
 */
public interface TranscriptionService {

    boolean isAvailable();

    Optional<Transcript> transcribe(Path wav16kMono);
}
