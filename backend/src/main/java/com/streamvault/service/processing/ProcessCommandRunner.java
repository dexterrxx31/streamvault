package com.streamvault.service.processing;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessCommandRunner implements CommandRunner {

    @Override
    public CommandResult run(List<String> command, Duration timeout) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();

        // Drain stdout/stderr concurrently to avoid pipe-buffer deadlock on large output
        CompletableFuture<String> stdout = readStream(process.getInputStream());
        CompletableFuture<String> stderr = readStream(process.getErrorStream());

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Command timed out after " + timeout.getSeconds() + "s: " + command.get(0));
        }

        try {
            return new CommandResult(process.exitValue(), stdout.get(), stderr.get());
        } catch (ExecutionException e) {
            throw new IOException("Failed to read process output", e.getCause());
        }
    }

    private CompletableFuture<String> readStream(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
