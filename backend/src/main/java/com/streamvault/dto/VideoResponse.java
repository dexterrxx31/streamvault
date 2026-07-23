package com.streamvault.dto;

import com.streamvault.model.VideoStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VideoResponse {
    private Long id;
    private String title;
    private String contentType;
    private Long size;
    private LocalDateTime uploadDate;
    private Double durationSeconds;
    private Integer width;
    private Integer height;
    private String description;
    private List<String> tags = new ArrayList<>();
    private VideoStatus status;
    private boolean hasThumbnail;

    public VideoResponse() {
    }

    public VideoResponse(Long id, String title, String contentType, Long size, LocalDateTime uploadDate,
            Double durationSeconds, Integer width, Integer height, String description, List<String> tags,
            VideoStatus status, boolean hasThumbnail) {
        this.id = id;
        this.title = title;
        this.contentType = contentType;
        this.size = size;
        this.uploadDate = uploadDate;
        this.durationSeconds = durationSeconds;
        this.width = width;
        this.height = height;
        this.description = description;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.status = status;
        this.hasThumbnail = hasThumbnail;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
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

    public VideoStatus getStatus() {
        return status;
    }

    public void setStatus(VideoStatus status) {
        this.status = status;
    }

    public boolean isHasThumbnail() {
        return hasThumbnail;
    }

    public void setHasThumbnail(boolean hasThumbnail) {
        this.hasThumbnail = hasThumbnail;
    }

    public static VideoResponseBuilder builder() {
        return new VideoResponseBuilder();
    }

    public static class VideoResponseBuilder {
        private Long id;
        private String title;
        private String contentType;
        private Long size;
        private LocalDateTime uploadDate;
        private Double durationSeconds;
        private Integer width;
        private Integer height;
        private String description;
        private List<String> tags;
        private VideoStatus status;
        private boolean hasThumbnail;

        public VideoResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public VideoResponseBuilder title(String title) {
            this.title = title;
            return this;
        }

        public VideoResponseBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public VideoResponseBuilder size(Long size) {
            this.size = size;
            return this;
        }

        public VideoResponseBuilder uploadDate(LocalDateTime uploadDate) {
            this.uploadDate = uploadDate;
            return this;
        }

        public VideoResponseBuilder durationSeconds(Double durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public VideoResponseBuilder width(Integer width) {
            this.width = width;
            return this;
        }

        public VideoResponseBuilder height(Integer height) {
            this.height = height;
            return this;
        }

        public VideoResponseBuilder description(String description) {
            this.description = description;
            return this;
        }

        public VideoResponseBuilder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public VideoResponseBuilder status(VideoStatus status) {
            this.status = status;
            return this;
        }

        public VideoResponseBuilder hasThumbnail(boolean hasThumbnail) {
            this.hasThumbnail = hasThumbnail;
            return this;
        }

        public VideoResponse build() {
            return new VideoResponse(id, title, contentType, size, uploadDate, durationSeconds, width, height,
                    description, tags, status, hasThumbnail);
        }
    }
}
