package com.streamvault.service.processing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Generates a poster thumbnail (640px wide, at 10% of duration) plus four
 * evenly spaced 480px frames reused later by the AI metadata step.
 */
@Component
@Order(20)
public class ThumbnailStep implements ProcessingStep {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final int FRAME_COUNT = 4;

    private final CommandRunner commandRunner;

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    public ThumbnailStep(CommandRunner commandRunner) {
        this.commandRunner = commandRunner;
    }

    @Override
    public String name() {
        return "thumbnail";
    }

    @Override
    public boolean required() {
        return true;
    }

    @Override
    public void process(ProcessingContext ctx) throws Exception {
        String baseName = stripExtension(ctx.getVideo().getFilename());
        double duration = ctx.getVideo().getDurationSeconds() != null
                ? ctx.getVideo().getDurationSeconds()
                : 0.0;

        // Poster thumbnail at 10% of duration (fallback: 1s into the video)
        double posterTime = duration > 0 ? duration * 0.1 : 1.0;
        String thumbnailFilename = baseName + "_thumb.jpg";
        Path thumbnailPath = ctx.getUploadDir().resolve(thumbnailFilename);
        extractFrame(ctx.getVideoPath(), posterTime, 640, thumbnailPath);
        ctx.getVideo().setThumbnailFilename(thumbnailFilename);

        // Evenly spaced frames for downstream AI analysis
        for (int i = 1; i <= FRAME_COUNT; i++) {
            double t = duration > 0 ? duration * i / (FRAME_COUNT + 1) : i;
            Path framePath = ctx.getUploadDir().resolve(baseName + "_frame_" + i + ".jpg");
            try {
                extractFrame(ctx.getVideoPath(), t, 480, framePath);
                ctx.getFrames().add(framePath);
            } catch (Exception e) {
                // Frames are best-effort; the poster thumbnail is the required output
            }
        }
    }

    private void extractFrame(Path video, double atSeconds, int width, Path output) throws Exception {
        List<String> command = List.of(
                ffmpegPath, "-y",
                "-ss", String.format(java.util.Locale.ROOT, "%.3f", atSeconds),
                "-i", video.toString(),
                "-vframes", "1",
                "-vf", "scale=" + width + ":-2",
                output.toString());

        CommandRunner.CommandResult result = commandRunner.run(command, TIMEOUT);
        if (!result.success() || !Files.exists(output)) {
            throw new RuntimeException("ffmpeg frame extraction failed (exit " + result.exitCode() + "): "
                    + result.stderr());
        }
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
