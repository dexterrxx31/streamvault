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
    private String aiTitle;
    private String aiDescription;
    private List<String> aiTags = new ArrayList<>();
    private String summary;
    private List<ChapterResponse> chapters = new ArrayList<>();
    private boolean hasCaptions;
    // Pre-signed, relative media URLs — only ever returned to the video's owner
    private String streamUrl;
    private String thumbnailUrl;
    private String captionsUrl;

    public VideoResponse() {
    }

    public VideoResponse(Long id, String title, String contentType, Long size, LocalDateTime uploadDate,
            Double durationSeconds, Integer width, Integer height, String description, List<String> tags,
            VideoStatus status, boolean hasThumbnail, String aiTitle, String aiDescription, List<String> aiTags,
            String summary, List<ChapterResponse> chapters, boolean hasCaptions) {
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
        this.aiTitle = aiTitle;
        this.aiDescription = aiDescription;
        this.aiTags = aiTags != null ? aiTags : new ArrayList<>();
        this.summary = summary;
        this.chapters = chapters != null ? chapters : new ArrayList<>();
        this.hasCaptions = hasCaptions;
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

    public String getAiTitle() {
        return aiTitle;
    }

    public void setAiTitle(String aiTitle) {
        this.aiTitle = aiTitle;
    }

    public String getAiDescription() {
        return aiDescription;
    }

    public void setAiDescription(String aiDescription) {
        this.aiDescription = aiDescription;
    }

    public List<String> getAiTags() {
        return aiTags;
    }

    public void setAiTags(List<String> aiTags) {
        this.aiTags = aiTags;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ChapterResponse> getChapters() {
        return chapters;
    }

    public void setChapters(List<ChapterResponse> chapters) {
        this.chapters = chapters;
    }

    public boolean isHasCaptions() {
        return hasCaptions;
    }

    public void setHasCaptions(boolean hasCaptions) {
        this.hasCaptions = hasCaptions;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getCaptionsUrl() {
        return captionsUrl;
    }

    public void setCaptionsUrl(String captionsUrl) {
        this.captionsUrl = captionsUrl;
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
        private String aiTitle;
        private String aiDescription;
        private List<String> aiTags;
        private String summary;
        private List<ChapterResponse> chapters;
        private boolean hasCaptions;
        private String streamUrl;
        private String thumbnailUrl;
        private String captionsUrl;

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

        public VideoResponseBuilder aiTitle(String aiTitle) {
            this.aiTitle = aiTitle;
            return this;
        }

        public VideoResponseBuilder aiDescription(String aiDescription) {
            this.aiDescription = aiDescription;
            return this;
        }

        public VideoResponseBuilder aiTags(List<String> aiTags) {
            this.aiTags = aiTags;
            return this;
        }

        public VideoResponseBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public VideoResponseBuilder chapters(List<ChapterResponse> chapters) {
            this.chapters = chapters;
            return this;
        }

        public VideoResponseBuilder hasCaptions(boolean hasCaptions) {
            this.hasCaptions = hasCaptions;
            return this;
        }

        public VideoResponseBuilder streamUrl(String streamUrl) {
            this.streamUrl = streamUrl;
            return this;
        }

        public VideoResponseBuilder thumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return this;
        }

        public VideoResponseBuilder captionsUrl(String captionsUrl) {
            this.captionsUrl = captionsUrl;
            return this;
        }

        public VideoResponse build() {
            VideoResponse response = new VideoResponse(id, title, contentType, size, uploadDate, durationSeconds,
                    width, height, description, tags, status, hasThumbnail, aiTitle, aiDescription, aiTags,
                    summary, chapters, hasCaptions);
            response.setStreamUrl(streamUrl);
            response.setThumbnailUrl(thumbnailUrl);
            response.setCaptionsUrl(captionsUrl);
            return response;
        }
    }
}
