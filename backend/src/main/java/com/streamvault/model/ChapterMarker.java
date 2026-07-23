package com.streamvault.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class ChapterMarker {

    private Double startSeconds;

    private String title;

    public ChapterMarker() {
    }

    public ChapterMarker(Double startSeconds, String title) {
        this.startSeconds = startSeconds;
        this.title = title;
    }

    public Double getStartSeconds() {
        return startSeconds;
    }

    public void setStartSeconds(Double startSeconds) {
        this.startSeconds = startSeconds;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
