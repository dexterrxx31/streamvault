package com.streamvault.repository;

import com.streamvault.model.Video;
import com.streamvault.model.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByUserIdOrderByUploadDateDesc(Long userId);

    List<Video> findByStatus(VideoStatus status);
}
