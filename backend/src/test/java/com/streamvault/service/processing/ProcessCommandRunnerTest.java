package com.streamvault.service.processing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessCommandRunner Tests")
class ProcessCommandRunnerTest {

    private final ProcessCommandRunner runner = new ProcessCommandRunner();

    @Test
    @DisplayName("Should capture stdout and exit code 0 on success")
    void run_success() throws Exception {
        CommandRunner.CommandResult result = runner.run(List.of("echo", "hello"), Duration.ofSeconds(5));

        assertTrue(result.success());
        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("hello"));
    }

    @Test
    @DisplayName("Should report non-zero exit code as failure")
    void run_failure() throws Exception {
        CommandRunner.CommandResult result = runner.run(List.of("false"), Duration.ofSeconds(5));

        assertFalse(result.success());
        assertNotEquals(0, result.exitCode());
    }

    @Test
    @DisplayName("Should kill process and throw on timeout")
    void run_timeout() {
        IOException exception = assertThrows(IOException.class,
                () -> runner.run(List.of("sleep", "10"), Duration.ofMillis(200)));
        assertTrue(exception.getMessage().contains("timed out"));
    }
}
