package com.streamvault.repository;

import com.streamvault.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByUserIdOrderByUploadDateDesc(Long userId);
}
