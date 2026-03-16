package com.streamvault.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Video Model Tests")
class VideoTest {

    @Test
    @DisplayName("Should create Video using builder")
    void builder_createsVideo() {
        User user = User.builder().id(1L).username("user").email("u@e.com").password("p").build();
        LocalDateTime now = LocalDateTime.now();

        Video video = Video.builder()
                .id(1L)
                .title("Test Video")
                .filename("test.mp4")
                .contentType("video/mp4")
                .size(2048L)
                .uploadDate(now)
                .user(user)
                .build();

        assertEquals(1L, video.getId());
        assertEquals("Test Video", video.getTitle());
        assertEquals("test.mp4", video.getFilename());
        assertEquals("video/mp4", video.getContentType());
        assertEquals(2048L, video.getSize());
        assertEquals(now, video.getUploadDate());
        assertEquals(user, video.getUser());
    }

    @Test
    @DisplayName("Should create Video with no-arg constructor")
    void noArgConstructor() {
        Video video = new Video();
        assertNull(video.getId());
        assertNull(video.getTitle());
        assertNull(video.getFilename());
        assertNull(video.getContentType());
        assertNull(video.getSize());
        assertNull(video.getUploadDate());
        assertNull(video.getUser());
    }

    @Test
    @DisplayName("Should set and get fields via setters/getters")
    void settersAndGetters() {
        Video video = new Video();
        User user = User.builder().id(2L).username("u").email("e@e.com").password("p").build();
        LocalDateTime date = LocalDateTime.of(2026, 3, 15, 12, 0);

        video.setId(10L);
        video.setTitle("Setter Video");
        video.setFilename("setter.webm");
        video.setContentType("video/webm");
        video.setSize(4096L);
        video.setUploadDate(date);
        video.setUser(user);

        assertEquals(10L, video.getId());
        assertEquals("Setter Video", video.getTitle());
        assertEquals("setter.webm", video.getFilename());
        assertEquals("video/webm", video.getContentType());
        assertEquals(4096L, video.getSize());
        assertEquals(date, video.getUploadDate());
        assertEquals(user, video.getUser());
    }
}
