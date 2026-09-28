package com.streamvault.service.processing;

import com.streamvault.model.ChapterMarker;
import com.streamvault.model.Video;
import com.streamvault.model.VideoStatus;
import com.streamvault.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Runs all {@link ProcessingStep} beans (in @Order sequence) for each uploaded
 * video on the bounded videoProcessingExecutor pool.
 *
 * Steps mutate an in-memory working copy of the video. After each step only
 * the pipeline-owned fields that the step actually changed are copied onto a
 * freshly loaded entity, so user edits made while processing runs (title,
 * tags, dismissing AI suggestions) are never overwritten. If the video was
 * deleted mid-run, processing stops and generated files are cleaned up.
 */
@Component
public class VideoProcessingPipeline {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingPipeline.class);
    private static final int SAVE_ATTEMPTS = 3;

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
            PipelineFields before = PipelineFields.of(video);
            try {
                log.info("Running step '{}' for video {}", step.name(), video.getId());
                step.process(ctx);
            } catch (Exception e) {
                if (step.required()) {
                    log.error("Required step '{}' failed for video {}", step.name(), video.getId(), e);
                    if (!updateStatus(video, VideoStatus.FAILED)) {
                        cleanUpDeleted(video, uploadPath);
                    }
                    return;
                }
                log.warn("Optional step '{}' failed for video {} — continuing", step.name(), video.getId(), e);
            }
            PipelineFields after = PipelineFields.of(video);
            if (!persist(video.getId(), fresh -> after.applyChangesSince(before, fresh))) {
                cleanUpDeleted(video, uploadPath);
                return;
            }
        }

        if (!updateStatus(video, VideoStatus.READY)) {
            cleanUpDeleted(video, uploadPath);
            return;
        }
        log.info("Processing complete for video {}", video.getId());
    }

    private boolean updateStatus(Video video, VideoStatus status) {
        video.setStatus(status);
        return persist(video.getId(), fresh -> fresh.setStatus(status));
    }

    /**
     * Applies {@code changes} to a freshly loaded copy and saves it, retrying
     * if a concurrent user edit bumped the version in between.
     *
     * @return false if the video no longer exists
     */
    private boolean persist(Long videoId, Consumer<Video> changes) {
        for (int attempt = 1; attempt <= SAVE_ATTEMPTS; attempt++) {
            Optional<Video> fresh = videoRepository.findById(videoId);
            if (fresh.isEmpty()) {
                return false;
            }
            changes.accept(fresh.get());
            try {
                videoRepository.save(fresh.get());
                return true;
            } catch (OptimisticLockingFailureException e) {
                log.debug("Concurrent update on video {} (attempt {}), retrying", videoId, attempt);
            } catch (RuntimeException e) {
                // Most likely deleted between load and save; don't crash the pool thread
                log.warn("Could not persist processing results for video {}", videoId, e);
                return videoRepository.existsById(videoId);
            }
        }
        log.warn("Gave up persisting processing results for video {} after {} attempts", videoId, SAVE_ATTEMPTS);
        return true;
    }

    private void cleanUpDeleted(Video video, Path uploadPath) {
        log.info("Video {} was deleted during processing — removing generated files", video.getId());
        for (String name : MediaFiles.derivedFilenames(video.getFilename())) {
            try {
                Files.deleteIfExists(uploadPath.resolve(name));
            } catch (IOException e) {
                log.warn("Could not delete {}", name, e);
            }
        }
    }

    /** Snapshot of the fields processing steps are allowed to write. */
    private record PipelineFields(Double durationSeconds, Integer width, Integer height,
            String thumbnailFilename, String aiTitle, String aiDescription, String aiTags,
            String captionsFilename, String transcriptText, String summary, List<ChapterMarker> chapters) {

        static PipelineFields of(Video v) {
            return new PipelineFields(v.getDurationSeconds(), v.getWidth(), v.getHeight(),
                    v.getThumbnailFilename(), v.getAiTitle(), v.getAiDescription(), v.getAiTags(),
                    v.getCaptionsFilename(), v.getTranscriptText(), v.getSummary(),
                    v.getChapters() == null ? null : new ArrayList<>(v.getChapters()));
        }

        void applyChangesSince(PipelineFields before, Video target) {
            if (!Objects.equals(durationSeconds, before.durationSeconds)) target.setDurationSeconds(durationSeconds);
            if (!Objects.equals(width, before.width)) target.setWidth(width);
            if (!Objects.equals(height, before.height)) target.setHeight(height);
            if (!Objects.equals(thumbnailFilename, before.thumbnailFilename)) target.setThumbnailFilename(thumbnailFilename);
            if (!Objects.equals(aiTitle, before.aiTitle)) target.setAiTitle(aiTitle);
            if (!Objects.equals(aiDescription, before.aiDescription)) target.setAiDescription(aiDescription);
            if (!Objects.equals(aiTags, before.aiTags)) target.setAiTags(aiTags);
            if (!Objects.equals(captionsFilename, before.captionsFilename)) target.setCaptionsFilename(captionsFilename);
            if (!Objects.equals(transcriptText, before.transcriptText)) target.setTranscriptText(transcriptText);
            if (!Objects.equals(summary, before.summary)) target.setSummary(summary);
            if (!Objects.equals(chapters, before.chapters)) target.setChapters(new ArrayList<>(chapters));
        }
    }
}
