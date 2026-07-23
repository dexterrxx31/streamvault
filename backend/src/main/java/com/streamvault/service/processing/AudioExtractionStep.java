package com.streamvault.service.processing;

import com.streamvault.service.transcription.TranscriptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Optional step: extracts a temp 16kHz mono WAV for transcription. Skips
 * entirely when transcription is unavailable — no point extracting audio.
 * The WAV is deleted by {@link TranscriptionStep} when it finishes.
 */
@Component
@Order(40)
public class AudioExtractionStep implements ProcessingStep {

    private static final Duration TIMEOUT = Duration.ofMinutes(10);

    private final CommandRunner commandRunner;
    private final TranscriptionService transcriptionService;

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    public AudioExtractionStep(CommandRunner commandRunner, TranscriptionService transcriptionService) {
        this.commandRunner = commandRunner;
        this.transcriptionService = transcriptionService;
    }

    @Override
    public String name() {
        return "audio-extraction";
    }

    @Override
    public boolean required() {
        return false;
    }

    @Override
    public void process(ProcessingContext ctx) throws Exception {
        if (!transcriptionService.isAvailable()) {
            return;
        }

        String baseName = stripExtension(ctx.getVideo().getFilename());
        Path wavPath = ctx.getUploadDir().resolve(baseName + "_audio.wav");

        List<String> command = List.of(
                ffmpegPath, "-y",
                "-i", ctx.getVideoPath().toString(),
                "-vn", "-ar", "16000", "-ac", "1", "-f", "wav",
                wavPath.toString());

        CommandRunner.CommandResult result = commandRunner.run(command, TIMEOUT);
        if (!result.success() || !Files.exists(wavPath)) {
            throw new RuntimeException("ffmpeg audio extraction failed (exit " + result.exitCode() + "): "
                    + result.stderr());
        }
        ctx.setAudioPath(wavPath);
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
