package com.streamvault.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;

/**
 * Signs short-lived media URLs (stream, thumbnail, captions). {@code <video>},
 * {@code <img>} and {@code <track>} tags cannot send an Authorization header,
 * so the owner receives pre-signed URLs from the authenticated API instead and
 * the media endpoints verify the signature.
 */
@Component
public class MediaUrlSigner {

    /** Expiry is rounded up to this bucket so URLs stay stable (cacheable) for a while. */
    private static final long BUCKET_SECONDS = 600;

    private final SecretKeySpec key;
    private final long ttlSeconds;
    private final Clock clock;

    @Autowired
    public MediaUrlSigner(@Value("${app.media.secret}") String secret,
            @Value("${app.media.url-ttl-seconds:7200}") long ttlSeconds) {
        this(secret, ttlSeconds, Clock.systemUTC());
    }

    MediaUrlSigner(String secret, long ttlSeconds, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.media.secret (MEDIA_SECRET) must be at least 32 bytes");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.ttlSeconds = ttlSeconds;
        this.clock = clock;
    }

    /** Returns {@code exp=...&sig=...} for the given video and media kind. */
    public String signQuery(long videoId, String kind) {
        long now = clock.instant().getEpochSecond();
        long exp = ((now + ttlSeconds) / BUCKET_SECONDS + 1) * BUCKET_SECONDS;
        return "exp=" + exp + "&sig=" + sign(videoId, kind, exp);
    }

    public boolean verify(long videoId, String kind, Long exp, String sig) {
        if (exp == null || sig == null || exp < clock.instant().getEpochSecond()) {
            return false;
        }
        byte[] expected = sign(videoId, kind, exp).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, sig.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(long videoId, String kind, long exp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] digest = mac.doFinal((videoId + "|" + kind + "|" + exp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
