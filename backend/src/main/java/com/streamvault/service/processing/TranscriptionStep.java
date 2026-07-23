package com.streamvault.service.processing;

import com.streamvault.service.transcription.Transcript;
import com.streamvault.service.transcription.TranscriptionService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Optional step: transcribes the extracted audio, stores the transcript on
 * the video, and writes WebVTT captions next to the video file. Always
 * deletes the temp WAV, success or not.
 */
@Component
@Order(50)
public class TranscriptionStep implements ProcessingStep {

    private final TranscriptionService transcriptionService;

    public TranscriptionStep(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @Override
    public String name() {
        return "transcription";
    }

    @Override
    public boolean required() {
        return false;
    }

    @Override
    public void process(ProcessingContext ctx) throws Exception {
        Path wavPath = ctx.getAudioPath();
        if (wavPath == null || !transcriptionService.isAvailable()) {
            return;
        }
        try {
            Optional<Transcript> maybeTranscript = transcriptionService.transcribe(wavPath);
            if (maybeTranscript.isEmpty() || maybeTranscript.get().isEmpty()) {
                return;
            }
            Transcript transcript = maybeTranscript.get();
            ctx.setTranscript(transcript);

            String baseName = stripExtension(ctx.getVideo().getFilename());
            String captionsFilename = baseName + ".vtt";
            Files.writeString(ctx.getUploadDir().resolve(captionsFilename),
                    transcript.toVtt(), StandardCharsets.UTF_8);

            ctx.getVideo().setCaptionsFilename(captionsFilename);
            ctx.getVideo().setTranscriptText(transcript.toPlainText());
        } finally {
            Files.deleteIfExists(wavPath);
        }
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
