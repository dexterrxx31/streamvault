package com.streamvault.exception;

/** HTTP 404 — also used for resources the caller does not own, so ids cannot be probed. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
