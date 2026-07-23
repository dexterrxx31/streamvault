package com.streamvault.dto;

import java.util.List;

public class UpdateVideoRequest {
    private String title;
    private String description;
    private List<String> tags;

    public UpdateVideoRequest() {
    }

    public UpdateVideoRequest(String title, String description, List<String> tags) {
        this.title = title;
        this.description = description;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
