package com.streamvault.service.processing;

/**
 * One stage of the async video post-processing pipeline. Steps are discovered
 * as Spring beans and executed in {@code @Order} sequence by
 * {@link VideoProcessingPipeline}. A failing required step marks the video
 * FAILED; a failing optional step is logged and skipped.
 */
public interface ProcessingStep {

    String name();

    boolean required();

    void process(ProcessingContext ctx) throws Exception;
}
