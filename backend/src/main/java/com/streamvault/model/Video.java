package com.streamvault.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    private Long size;

    @Column(nullable = false)
    private LocalDateTime uploadDate;

    private Double durationSeconds;

    private Integer width;

    private Integer height;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "video_tags", joinColumns = @JoinColumn(name = "video_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status;

    private String thumbnailFilename;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        this.uploadDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = VideoStatus.PROCESSING;
        }
    }

    public Video() {
    }

    public Video(Long id, String title, String filename, String contentType, Long size, LocalDateTime uploadDate,
            Double durationSeconds, Integer width, Integer height, String description, List<String> tags,
            VideoStatus status, String thumbnailFilename, User user) {
        this.id = id;
        this.title = title;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.uploadDate = uploadDate;
        this.durationSeconds = durationSeconds;
        this.width = width;
        this.height = height;
        this.description = description;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.status = status;
        this.thumbnailFilename = thumbnailFilename;
        this.user = user;
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

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
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

    public String getThumbnailFilename() {
        return thumbnailFilename;
    }

    public void setThumbnailFilename(String thumbnailFilename) {
        this.thumbnailFilename = thumbnailFilename;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public static VideoBuilder builder() {
        return new VideoBuilder();
    }

    public static class VideoBuilder {
        private Long id;
        private String title;
        private String filename;
        private String contentType;
        private Long size;
        private LocalDateTime uploadDate;
        private Double durationSeconds;
        private Integer width;
        private Integer height;
        private String description;
        private List<String> tags;
        private VideoStatus status;
        private String thumbnailFilename;
        private User user;

        public VideoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public VideoBuilder title(String title) {
            this.title = title;
            return this;
        }

        public VideoBuilder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public VideoBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public VideoBuilder size(Long size) {
            this.size = size;
            return this;
        }

        public VideoBuilder uploadDate(LocalDateTime uploadDate) {
            this.uploadDate = uploadDate;
            return this;
        }

        public VideoBuilder durationSeconds(Double durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public VideoBuilder width(Integer width) {
            this.width = width;
            return this;
        }

        public VideoBuilder height(Integer height) {
            this.height = height;
            return this;
        }

        public VideoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public VideoBuilder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public VideoBuilder status(VideoStatus status) {
            this.status = status;
            return this;
        }

        public VideoBuilder thumbnailFilename(String thumbnailFilename) {
            this.thumbnailFilename = thumbnailFilename;
            return this;
        }

        public VideoBuilder user(User user) {
            this.user = user;
            return this;
        }

        public Video build() {
            return new Video(id, title, filename, contentType, size, uploadDate, durationSeconds, width, height,
                    description, tags, status, thumbnailFilename, user);
        }
    }
}
