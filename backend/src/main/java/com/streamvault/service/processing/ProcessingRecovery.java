package com.streamvault.service.processing;

import com.streamvault.model.Video;
import com.streamvault.model.VideoStatus;
import com.streamvault.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Processing jobs live only in the in-memory executor queue, so any video
 * still PROCESSING at startup was interrupted by a restart and would otherwise
 * stay stuck forever. Mark those FAILED so the UI stops polling for them.
 */
@Component
public class ProcessingRecovery {

    private static final Logger log = LoggerFactory.getLogger(ProcessingRecovery.class);

    private final VideoRepository videoRepository;

    public ProcessingRecovery(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedProcessing() {
        List<Video> stuck = videoRepository.findByStatus(VideoStatus.PROCESSING);
        for (Video video : stuck) {
            video.setStatus(VideoStatus.FAILED);
        }
        if (!stuck.isEmpty()) {
            videoRepository.saveAll(stuck);
            log.warn("Marked {} video(s) interrupted by a restart as FAILED", stuck.size());
        }
    }
}
