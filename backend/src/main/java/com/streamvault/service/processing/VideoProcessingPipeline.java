package com.streamvault.service.processing;

import com.streamvault.model.Video;
import com.streamvault.model.VideoStatus;
import com.streamvault.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Runs all {@link ProcessingStep} beans (in @Order sequence) for each uploaded
 * video on the bounded videoProcessingExecutor pool.
 */
@Component
public class VideoProcessingPipeline {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingPipeline.class);

    private final VideoRepository videoRepository;
    private final List<ProcessingStep> steps;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public VideoProcessingPipeline(VideoRepository videoRepository, List<ProcessingStep> steps) {
        this.videoRepository = videoRepository;
        this.steps = steps;
    }

    @Async("videoProcessingExecutor")
    @EventListener
    public void onVideoUploaded(VideoUploadedEvent event) {
        Optional<Video> maybeVideo = videoRepository.findById(event.videoId());
        if (maybeVideo.isEmpty()) {
            // Video was deleted before processing started — nothing to do
            log.info("Skipping processing for deleted video {}", event.videoId());
            return;
        }
        Video video = maybeVideo.get();

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path videoPath = uploadPath.resolve(video.getFilename());
        if (!Files.exists(videoPath)) {
            log.warn("Video file missing for video {}: {}", video.getId(), videoPath);
            updateStatus(video, VideoStatus.FAILED);
            return;
        }

        ProcessingContext ctx = new ProcessingContext(video, videoPath, uploadPath);

        for (ProcessingStep step : steps) {
            try {
                log.info("Running step '{}' for video {}", step.name(), video.getId());
                step.process(ctx);
                videoRepository.save(video);
            } catch (Exception e) {
                if (step.required()) {
                    log.error("Required step '{}' failed for video {}", step.name(), video.getId(), e);
                    updateStatus(video, VideoStatus.FAILED);
                    return;
                }
                log.warn("Optional step '{}' failed for video {} — continuing", step.name(), video.getId(), e);
            }
        }

        updateStatus(video, VideoStatus.READY);
        log.info("Processing complete for video {}", video.getId());
    }

    private void updateStatus(Video video, VideoStatus status) {
        video.setStatus(status);
        try {
            videoRepository.save(video);
        } catch (RuntimeException e) {
            // Video may have been deleted mid-processing; don't crash the pool thread
            log.warn("Could not persist status {} for video {}", status, video.getId(), e);
        }
    }
}
