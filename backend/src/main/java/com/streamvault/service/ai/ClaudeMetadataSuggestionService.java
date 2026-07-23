package com.streamvault.service.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Suggests metadata by sending sampled video frames to Claude with a
 * structured-output schema. Fully inert when ANTHROPIC_API_KEY is unset —
 * a missing key must never fail application startup or video processing.
 */
@Service
public class ClaudeMetadataSuggestionService implements MetadataSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeMetadataSuggestionService.class);

    private final AnthropicClient client;

    public ClaudeMetadataSuggestionService() {
        // fromEnv() resolves ANTHROPIC_API_KEY, ANTHROPIC_AUTH_TOKEN, or an
        // `ant auth login` profile; it throws when no credentials exist.
        AnthropicClient resolved = null;
        try {
            resolved = AnthropicOkHttpClient.fromEnv();
        } catch (Exception e) {
            log.info("No Anthropic credentials found — AI metadata suggestions disabled");
        }
        this.client = resolved;
    }

    @Override
    public Optional<SuggestedMetadata> suggestMetadata(List<Path> frames, String currentTitle) {
        if (client == null || frames.isEmpty()) {
            return Optional.empty();
        }
        try {
            List<ContentBlockParam> blocks = new ArrayList<>();
            for (Path frame : frames) {
                String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(frame));
                blocks.add(ContentBlockParam.ofImage(ImageBlockParam.builder()
                        .source(Base64ImageSource.builder()
                                .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                                .data(base64)
                                .build())
                        .build()));
            }
            blocks.add(ContentBlockParam.ofText(TextBlockParam.builder()
                    .text("These are evenly spaced frames from a video the user titled \""
                            + currentTitle + "\". Based on what the frames show, suggest an improved "
                            + "title, a 1-3 sentence description, and 3-6 short tags.")
                    .build()));

            StructuredMessageCreateParams<SuggestedMetadata> params = MessageCreateParams.builder()
                    // String form — this SDK release predates the CLAUDE_OPUS_4_8 constant
                    .model("claude-opus-4-8")
                    .maxTokens(1024L)
                    .thinking(ThinkingConfigAdaptive.builder().build())
                    .outputConfig(SuggestedMetadata.class)
                    .addUserMessageOfBlockParams(blocks)
                    .build();

            return client.messages().create(params).content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(typed -> typed.text())
                    .findFirst();
        } catch (Exception e) {
            log.warn("AI metadata suggestion failed — continuing without suggestions", e);
            return Optional.empty();
        }
    }
}
