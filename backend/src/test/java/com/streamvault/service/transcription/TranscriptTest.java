package com.streamvault.service.transcription;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Transcript Tests")
class TranscriptTest {

    private final Transcript transcript = new Transcript(List.of(
            new Transcript.Segment(0.0, 1.44, " Welcome to StreamVault."),
            new Transcript.Segment(1.44, 3675.5, " This runs for over an hour.")));

    @Test
    @DisplayName("Should format WebVTT with HH:MM:SS.mmm timestamps in cue order")
    void toVtt() {
        String vtt = transcript.toVtt();

        assertTrue(vtt.startsWith("WEBVTT\n\n"));
        assertTrue(vtt.contains("00:00:00.000 --> 00:00:01.440\nWelcome to StreamVault."));
        assertTrue(vtt.contains("00:00:01.440 --> 01:01:15.500\nThis runs for over an hour."));
        assertTrue(vtt.indexOf("Welcome") < vtt.indexOf("hour"));
    }

    @Test
    @DisplayName("Should join trimmed segments as plain text")
    void toPlainText() {
        assertEquals("Welcome to StreamVault. This runs for over an hour.",
                transcript.toPlainText());
    }

    @Test
    @DisplayName("Should produce timestamped lines for LLM input")
    void toTimestampedText() {
        String text = transcript.toTimestampedText();
        assertTrue(text.contains("[0.0s] Welcome to StreamVault."));
        assertTrue(text.contains("[1.4s] This runs for over an hour."));
    }

    @Test
    @DisplayName("Should report empty when no segments")
    void isEmpty() {
        assertTrue(new Transcript(List.of()).isEmpty());
        assertFalse(transcript.isEmpty());
    }
}
