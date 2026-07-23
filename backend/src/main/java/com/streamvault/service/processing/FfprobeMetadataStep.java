package com.streamvault.service.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Extracts duration and resolution via ffprobe JSON output.
 */
@Component
@Order(10)
public class FfprobeMetadataStep implements ProcessingStep {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

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
                "-print_format", "json",
                "-show_format", "-show_streams",
                ctx.getVideoPath().toString());

        CommandRunner.CommandResult result = commandRunner.run(command, TIMEOUT);
        if (!result.success()) {
            throw new RuntimeException("ffprobe failed with exit code " + result.exitCode() + ": " + result.stderr());
        }

        JsonNode root = objectMapper.readTree(result.stdout());

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
