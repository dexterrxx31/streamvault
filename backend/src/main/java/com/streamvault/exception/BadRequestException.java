package com.streamvault.exception;

/** HTTP 400 — the message is written for the client and is returned as-is. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
