package com.streamvault.service.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Extracts duration and resolution via ffprobe JSON output. Also the
 * pipeline's gatekeeper: ffmpeg detects formats by content, not extension, so
 * an upload that is really e.g. an HLS playlist or concat script (which can
 * make ffmpeg fetch URLs or read local files) is rejected here, before any
 * later step decodes it.
 */
@Component
@Order(10)
public class FfprobeMetadataStep implements ProcessingStep {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /** ffprobe format_name components for the containers we accept. */
    private static final Set<String> ALLOWED_FORMATS = Set.of("mov", "mp4", "matroska", "webm", "avi");

    private final CommandRunner commandRunner;
    private final ObjectMapper objectMapper;

    @Value("${app.ffprobe.path:ffprobe}")
    private String ffprobePath;

    public FfprobeMetadataStep(CommandRunner commandRunner, ObjectMapper objectMapper) {
        this.commandRunner = commandRunner;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "ffprobe-metadata";
    }

    @Override
    public boolean required() {
        return true;
    }

    @Override
    public void process(ProcessingContext ctx) throws Exception {
        List<String> command = List.of(
                ffprobePath, "-v", "quiet",
                "-protocol_whitelist", "file",
                "-print_format", "json",
                "-show_format", "-show_streams",
                ctx.getVideoPath().toString());

        CommandRunner.CommandResult result = commandRunner.run(command, TIMEOUT);
        if (!result.success()) {
            throw new RuntimeException("ffprobe failed with exit code " + result.exitCode() + ": " + result.stderr());
        }

        JsonNode root = objectMapper.readTree(result.stdout());

        String formatName = root.path("format").path("format_name").asText("");
        if (Arrays.stream(formatName.split(",")).noneMatch(ALLOWED_FORMATS::contains)) {
            throw new RuntimeException("Unsupported container format: '" + formatName + "'");
        }

        JsonNode duration = root.path("format").path("duration");
        if (!duration.isMissingNode()) {
            ctx.getVideo().setDurationSeconds(duration.asDouble());
        }

        for (JsonNode stream : root.path("streams")) {
            if ("video".equals(stream.path("codec_type").asText())) {
                ctx.getVideo().setWidth(stream.path("width").asInt());
                ctx.getVideo().setHeight(stream.path("height").asInt());
                break;
            }
        }
    }
}
