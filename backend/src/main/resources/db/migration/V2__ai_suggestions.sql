-- AI-suggested metadata (ephemeral until the user accepts or dismisses)

ALTER TABLE videos
    ADD COLUMN ai_title VARCHAR(255),
    ADD COLUMN ai_description TEXT,
    ADD COLUMN ai_tags TEXT;
