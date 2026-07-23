package com.streamvault.service.transcription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamvault.service.processing.CommandRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WhisperCliTranscriptionService Tests")
class WhisperCliTranscriptionServiceTest {

    /** Real whisper.cpp -oj output shape: offsets in milliseconds. */
    private static final String WHISPER_JSON = """
            {
              "systeminfo": "...",
              "transcription": [
                {"timestamps": {"from": "00:00:00,000", "to": "00:00:01,440"},
                 "offsets": {"from": 0, "to": 1440}, "text": " Welcome to StreamVault."},
                {"timestamps": {"from": "00:00:01,440", "to": "00:00:04,000"},
                 "offsets": {"from": 1440, "to": 4000}, "text": " Three topics today."}
              ]
            }
            """;

    @Mock
    private CommandRunner commandRunner;

    @TempDir
    Path tempDir;

    private WhisperCliTranscriptionService service;
    private Path modelFile;
    private Path wavFile;

    @BeforeEach
    void setUp() throws Exception {
        service = new WhisperCliTranscriptionService(commandRunner, new ObjectMapper());
        modelFile = tempDir.resolve("ggml-base.en.bin");
        Files.write(modelFile, new byte[] { 1 });
        wavFile = tempDir.resolve("audio.wav");
        Files.write(wavFile, new byte[] { 1 });
        ReflectionTestUtils.setField(service, "whisperPath", "whisper-cli");
        ReflectionTestUtils.setField(service, "modelPath", modelFile.toString());
    }

    @Test
    @DisplayName("Should parse whisper.cpp JSON (offsets in ms) into seconds")
    void transcribe_success() throws Exception {
        when(commandRunner.run(anyList(), any(Duration.class))).thenAnswer(invocation -> {
            // whisper-cli writes <basename>.json for -of <basename>
            Files.writeString(tempDir.resolve("audio.json"), WHISPER_JSON);
            return new CommandRunner.CommandResult(0, "", "");
        });

        Optional<Transcript> result = service.transcribe(wavFile);

        assertTrue(result.isPresent());
        List<Transcript.Segment> segments = result.get().segments();
        assertEquals(2, segments.size());
        assertEquals(0.0, segments.get(0).start());
        assertEquals(1.44, segments.get(0).end());
        assertEquals(" Welcome to StreamVault.", segments.get(0).text());
        assertEquals(4.0, segments.get(1).end());
        // Temp JSON is cleaned up
        assertFalse(Files.exists(tempDir.resolve("audio.json")));
    }

    @Test
    @DisplayName("Should return empty when whisper exits non-zero")
    void transcribe_whisperFails() throws Exception {
        when(commandRunner.run(anyList(), any(Duration.class)))
                .thenReturn(new CommandRunner.CommandResult(1, "", "model load failed"));

        assertTrue(service.transcribe(wavFile).isEmpty());
    }

    @Test
    @DisplayName("Should be unavailable when model path is empty")
    void isAvailable_noModelPath() throws Exception {
        ReflectionTestUtils.setField(service, "modelPath", "");

        assertFalse(service.isAvailable());
        assertTrue(service.transcribe(wavFile).isEmpty());
        verify(commandRunner, never()).run(anyList(), any());
    }

    @Test
    @DisplayName("Should be unavailable when model file does not exist")
    void isAvailable_modelMissing() {
        ReflectionTestUtils.setField(service, "modelPath", tempDir.resolve("nope.bin").toString());

        assertFalse(service.isAvailable());
    }

    @Test
    @DisplayName("Should be available with binary and existing model")
    void isAvailable_configured() {
        assertTrue(service.isAvailable());
    }
}
