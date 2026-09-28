-- Optimistic-locking version column (JPA @Version) for videos

ALTER TABLE videos
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
