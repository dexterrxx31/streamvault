package com.streamvault.service.processing;

import com.streamvault.model.Video;
import com.streamvault.service.transcription.Transcript;

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
    private Path audioPath;
    private Transcript transcript;

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

    /** Temp 16kHz mono WAV produced by the audio-extraction step. */
    public Path getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(Path audioPath) {
        this.audioPath = audioPath;
    }

    /** Transcript produced by the transcription step, consumed by chapters. */
    public Transcript getTranscript() {
        return transcript;
    }

    public void setTranscript(Transcript transcript) {
        this.transcript = transcript;
    }
}
