-- Transcription (WebVTT captions + transcript text) and AI-generated chapters

ALTER TABLE videos
    ADD COLUMN captions_filename VARCHAR(255),
    ADD COLUMN transcript_text TEXT,
    ADD COLUMN summary TEXT;

CREATE TABLE video_chapters (
    video_id      BIGINT NOT NULL REFERENCES videos (id),
    position      INTEGER NOT NULL,
    start_seconds DOUBLE PRECISION,
    title         VARCHAR(255),
    PRIMARY KEY (video_id, position)
);
