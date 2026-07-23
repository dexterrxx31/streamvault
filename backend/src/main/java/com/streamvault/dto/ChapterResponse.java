package com.streamvault.dto;

public class ChapterResponse {
    private Double startSeconds;
    private String title;

    public ChapterResponse() {
    }

    public ChapterResponse(Double startSeconds, String title) {
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
