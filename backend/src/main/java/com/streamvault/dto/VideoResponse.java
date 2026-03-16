package com.streamvault.dto;

import java.time.LocalDateTime;

public class VideoResponse {
    private Long id;
    private String title;
    private String contentType;
    private Long size;
    private LocalDateTime uploadDate;

    public VideoResponse() {
    }

    public VideoResponse(Long id, String title, String contentType, Long size, LocalDateTime uploadDate) {
        this.id = id;
        this.title = title;
        this.contentType = contentType;
        this.size = size;
        this.uploadDate = uploadDate;
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

    public static VideoResponseBuilder builder() {
        return new VideoResponseBuilder();
    }

    public static class VideoResponseBuilder {
        private Long id;
        private String title;
        private String contentType;
        private Long size;
        private LocalDateTime uploadDate;

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

        public VideoResponse build() {
            return new VideoResponse(id, title, contentType, size, uploadDate);
        }
    }
}
