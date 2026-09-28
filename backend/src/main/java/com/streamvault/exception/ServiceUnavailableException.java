package com.streamvault.exception;

/** HTTP 503 — the server cannot take more work right now. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
