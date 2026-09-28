package com.streamvault.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public class UpdateVideoRequest {
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @Size(max = 20)
    private List<@Size(max = 50) String> tags;

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
