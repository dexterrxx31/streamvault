package com.streamvault.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MediaUrlSigner Tests")
class MediaUrlSignerTest {

    private static final String SECRET = "test-media-secret-that-is-at-least-32-bytes";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private MediaUrlSigner signerAt(Instant instant) {
        return new MediaUrlSigner(SECRET, 3600, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static long exp(String query) {
        return Long.parseLong(query.split("&")[0].substring("exp=".length()));
    }

    private static String sig(String query) {
        return query.split("&")[1].substring("sig=".length());
    }

    @Test
    @DisplayName("Should verify its own signature for the same id and kind only")
    void signAndVerify() {
        MediaUrlSigner signer = signerAt(NOW);
        String q = signer.signQuery(42L, "stream");

        assertTrue(signer.verify(42L, "stream", exp(q), sig(q)));
        assertFalse(signer.verify(43L, "stream", exp(q), sig(q)));
        assertFalse(signer.verify(42L, "thumbnail", exp(q), sig(q)));
        assertFalse(signer.verify(42L, "stream", exp(q) + 600, sig(q)));
        assertFalse(signer.verify(42L, "stream", exp(q), "forged"));
        assertFalse(signer.verify(42L, "stream", null, null));
    }

    @Test
    @DisplayName("Should reject expired URLs")
    void expired() {
        String q = signerAt(NOW).signQuery(1L, "stream");

        assertFalse(signerAt(NOW.plus(Duration.ofHours(3))).verify(1L, "stream", exp(q), sig(q)));
    }

    @Test
    @DisplayName("Should keep URLs stable within an expiry bucket so thumbnails stay cacheable")
    void stableWithinBucket() {
        String a = signerAt(NOW).signQuery(1L, "thumbnail");
        String b = signerAt(NOW.plusSeconds(30)).signQuery(1L, "thumbnail");

        assertEquals(a, b);
        assertTrue(exp(a) >= NOW.getEpochSecond() + 3600);
    }

    @Test
    @DisplayName("Should refuse a short secret")
    void shortSecret() {
        assertThrows(IllegalStateException.class, () -> new MediaUrlSigner("too-short", 3600));
    }
}
