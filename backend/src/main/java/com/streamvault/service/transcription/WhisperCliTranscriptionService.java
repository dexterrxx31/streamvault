package com.streamvault.service.transcription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamvault.service.processing.CommandRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Transcribes audio with the whisper.cpp CLI ({@code whisper-cli}). Disabled
 * unless both the binary and a model path are configured
 * ({@code app.whisper.path} / {@code app.whisper.model.path}).
 *
 * The whisper.cpp JSON shape ({@code transcription[].offsets.from/to} in
 * milliseconds) is parsed privately here — swapping to another CLI (e.g.
 * openai-whisper, which uses {@code segments[].start/end} in seconds) means
 * changing only this class.
 */
@Service
public class WhisperCliTranscriptionService implements TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(WhisperCliTranscriptionService.class);
    private static final Duration TIMEOUT = Duration.ofMinutes(20);

    private final CommandRunner commandRunner;
    private final ObjectMapper objectMapper;

    @Value("${app.whisper.path:whisper-cli}")
    private String whisperPath;

    @Value("${app.whisper.model.path:}")
    private String modelPath;

    public WhisperCliTranscriptionService(CommandRunner commandRunner, ObjectMapper objectMapper) {
        this.commandRunner = commandRunner;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isAvailable() {
        return whisperPath != null && !whisperPath.isBlank()
                && modelPath != null && !modelPath.isBlank()
                && Files.exists(Path.of(modelPath));
    }

    @Override
    public Optional<Transcript> transcribe(Path wav16kMono) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        // whisper-cli -of takes a basename and writes <basename>.json
        Path outputBase = wav16kMono.resolveSibling(stripExtension(wav16kMono.getFileName().toString()));
        Path outputJson = outputBase.resolveSibling(outputBase.getFileName() + ".json");
        try {
            List<String> command = List.of(
                    whisperPath,
                    "-m", modelPath,
                    "-f", wav16kMono.toString(),
                    "-oj",
                    "-of", outputBase.toString());

            CommandRunner.CommandResult result = commandRunner.run(command, TIMEOUT);
            if (!result.success() || !Files.exists(outputJson)) {
                log.warn("whisper-cli failed (exit {}): {}", result.exitCode(), result.stderr());
                return Optional.empty();
            }

            return Optional.of(parseWhisperJson(Files.readString(outputJson)));
        } catch (Exception e) {
            log.warn("Transcription failed", e);
            return Optional.empty();
        } finally {
            try {
                Files.deleteIfExists(outputJson);
            } catch (Exception ignored) {
            }
        }
    }

    private Transcript parseWhisperJson(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<Transcript.Segment> segments = new ArrayList<>();
        for (JsonNode segment : root.path("transcription")) {
            String text = segment.path("text").asText("");
            if (text.isBlank()) {
                continue;
            }
            // whisper.cpp offsets are in milliseconds
            double start = segment.path("offsets").path("from").asLong() / 1000.0;
            double end = segment.path("offsets").path("to").asLong() / 1000.0;
            segments.add(new Transcript.Segment(start, end, text));
        }
        return new Transcript(segments);
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
