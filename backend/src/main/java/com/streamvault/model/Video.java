package com.streamvault.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        this.uploadDate = LocalDateTime.now();
    }

    public Video() {
    }

    public Video(Long id, String title, String filename, String contentType, Long size, LocalDateTime uploadDate,
            User user) {
        this.id = id;
        this.title = title;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.uploadDate = uploadDate;
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

        public VideoBuilder user(User user) {
            this.user = user;
            return this;
        }

        public Video build() {
            return new Video(id, title, filename, contentType, size, uploadDate, user);
        }
    }
}
