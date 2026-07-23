package com.streamvault.service.processing;

import com.streamvault.model.Video;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable state shared across pipeline steps for a single video.
 * Steps mutate {@link #getVideo()}; the pipeline persists it after each step.
 */
public class ProcessingContext {

    private final Video video;
    private final Path videoPath;
    private final Path uploadDir;
    private final List<Path> frames = new ArrayList<>();

    public ProcessingContext(Video video, Path videoPath, Path uploadDir) {
        this.video = video;
        this.videoPath = videoPath;
        this.uploadDir = uploadDir;
    }

    public Video getVideo() {
        return video;
    }

    public Path getVideoPath() {
        return videoPath;
    }

    public Path getUploadDir() {
        return uploadDir;
    }

    /** Frames extracted by the thumbnail step, reused by later (AI) steps. */
    public List<Path> getFrames() {
        return frames;
    }
}
