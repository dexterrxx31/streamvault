package com.streamvault.service.processing;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Seam around external process execution (ffmpeg, ffprobe, whisper, ...).
 * Tests mock this interface so no external binaries are needed in CI.
 */
public interface CommandRunner {

    CommandResult run(List<String> command, Duration timeout) throws IOException, InterruptedException;

    record CommandResult(int exitCode, String stdout, String stderr) {
        public boolean success() {
            return exitCode == 0;
        }
    }
}
