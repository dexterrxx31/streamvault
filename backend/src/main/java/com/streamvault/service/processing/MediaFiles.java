package com.streamvault.service.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Naming conventions for a stored video and the files derived from it, plus
 * the allowlist of accepted video extensions. Content types are always derived
 * from the (server-chosen) extension, never from what the client claimed.
 */
public final class MediaFiles {

    public static final int FRAME_COUNT = 4;

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "mp4", "video/mp4",
            "m4v", "video/mp4",
            "webm", "video/webm",
            "mov", "video/quicktime",
            "mkv", "video/x-matroska",
            "avi", "video/x-msvideo");

    private MediaFiles() {
    }

    /** Lowercase extension without the dot, if it is an allowed video type. */
    public static Optional<String> allowedExtension(String filename) {
        if (filename == null) {
            return Optional.empty();
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return CONTENT_TYPES.containsKey(ext) ? Optional.of(ext) : Optional.empty();
    }

    /** Content type for a stored filename; octet-stream for anything unknown. */
    public static String contentTypeFor(String filename) {
        return allowedExtension(filename).map(CONTENT_TYPES::get).orElse("application/octet-stream");
    }

    public static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    public static String thumbnailName(String videoFilename) {
        return stripExtension(videoFilename) + "_thumb.jpg";
    }

    public static String frameName(String videoFilename, int index) {
        return stripExtension(videoFilename) + "_frame_" + index + ".jpg";
    }

    public static String captionsName(String videoFilename) {
        return stripExtension(videoFilename) + ".vtt";
    }

    public static String audioName(String videoFilename) {
        return stripExtension(videoFilename) + "_audio.wav";
    }

    /** Every file the processing pipeline may create next to the video. */
    public static List<String> derivedFilenames(String videoFilename) {
        List<String> names = new ArrayList<>();
        names.add(thumbnailName(videoFilename));
        for (int i = 1; i <= FRAME_COUNT; i++) {
            names.add(frameName(videoFilename, i));
        }
        names.add(captionsName(videoFilename));
        names.add(audioName(videoFilename));
        return names;
    }
}
