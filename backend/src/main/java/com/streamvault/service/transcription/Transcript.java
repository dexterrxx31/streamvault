package com.streamvault.service.transcription;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A timestamped transcript. Times are in seconds.
 */
public record Transcript(List<Segment> segments) {

    public record Segment(double start, double end, String text) {
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    public String toPlainText() {
        return segments.stream()
                .map(s -> s.text().trim())
                .filter(t -> !t.isEmpty())
                .collect(Collectors.joining(" "));
    }

    /** Timestamped lines for LLM input, e.g. {@code [12.5s] Hello there}. */
    public String toTimestampedText() {
        return segments.stream()
                .map(s -> String.format(Locale.ROOT, "[%.1fs] %s", s.start(), s.text().trim()))
                .collect(Collectors.joining("\n"));
    }

    public String toVtt() {
        StringBuilder vtt = new StringBuilder("WEBVTT\n\n");
        for (Segment segment : segments) {
            vtt.append(formatTimestamp(segment.start()))
                    .append(" --> ")
                    .append(formatTimestamp(segment.end()))
                    .append('\n')
                    .append(segment.text().trim())
                    .append("\n\n");
        }
        return vtt.toString();
    }

    private static String formatTimestamp(double seconds) {
        long totalMillis = Math.round(seconds * 1000);
        long h = totalMillis / 3_600_000;
        long m = (totalMillis % 3_600_000) / 60_000;
        long s = (totalMillis % 60_000) / 1000;
        long ms = totalMillis % 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", h, m, s, ms);
    }
}
