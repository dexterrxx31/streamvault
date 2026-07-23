package com.streamvault.service.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.streamvault.service.transcription.Transcript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Generates summary + chapters from a transcript via Claude with a
 * structured-output schema. Same guarded-client pattern as
 * {@link ClaudeMetadataSuggestionService}: fully inert without credentials.
 */
@Service
public class ClaudeChapterSuggestionService implements ChapterSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeChapterSuggestionService.class);

    /** Chapters only need coarse structure — cap very long transcripts. */
    private static final int MAX_TRANSCRIPT_CHARS = 50_000;

    private final AnthropicClient client;

    public ClaudeChapterSuggestionService() {
        AnthropicClient resolved = null;
        try {
            resolved = AnthropicOkHttpClient.fromEnv();
        } catch (Exception e) {
            log.info("No Anthropic credentials found — AI chapter generation disabled");
        }
        this.client = resolved;
    }

    @Override
    public Optional<ChapterSuggestions> suggestChapters(Transcript transcript, String title) {
        if (client == null || transcript == null || transcript.isEmpty()) {
            return Optional.empty();
        }
        try {
            String timestamped = transcript.toTimestampedText();
            if (timestamped.length() > MAX_TRANSCRIPT_CHARS) {
                timestamped = timestamped.substring(0, MAX_TRANSCRIPT_CHARS);
            }

            StructuredMessageCreateParams<ChapterSuggestions> params = MessageCreateParams.builder()
                    // String form — this SDK release predates the CLAUDE_OPUS_4_8 constant
                    .model("claude-opus-4-8")
                    .maxTokens(2048L)
                    .thinking(ThinkingConfigAdaptive.builder().build())
                    .outputConfig(ChapterSuggestions.class)
                    .addUserMessage("Below is the timestamped transcript of a video titled \"" + title
                            + "\". Write a short summary and divide the video into chapters. "
                            + "Chapter start times must come from the transcript timestamps.\n\n"
                            + timestamped)
                    .build();

            return client.messages().create(params).content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(typed -> typed.text())
                    .findFirst();
        } catch (Exception e) {
            log.warn("AI chapter generation failed — continuing without chapters", e);
            return Optional.empty();
        }
    }
}
