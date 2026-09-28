package com.streamvault.exception;

/** HTTP 429 — the caller is temporarily rate limited. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
