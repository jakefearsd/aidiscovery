package com.jakefear.aidiscovery.autonomous;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakefear.aidiscovery.domain.ScopeConfiguration;
import com.jakefear.aidiscovery.util.JsonParsingUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Infers scope configuration from user's domain and description.
 * Uses AI to determine assumed knowledge, out-of-scope topics, focus areas, and audience.
 */
@Component
public class AutonomousScopeInferrer {

    private static final Logger log = LoggerFactory.getLogger(AutonomousScopeInferrer.class);

    private final ChatLanguageModel model;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        You are an expert knowledge architect helping to plan a comprehensive wiki.
        Your task is to analyze a domain and user description to infer the scope boundaries
        for the wiki content.

        You must determine:
        1. TARGET AUDIENCE - Who will read this wiki? What's their experience level?
        2. ASSUMED KNOWLEDGE - What concepts should readers already understand?
        3. OUT OF SCOPE - What topics should be explicitly excluded?
        4. FOCUS AREAS - What aspects should be emphasized?
        5. SEED TOPICS - 3-5 foundational topics to start content generation

        Be specific and practical. Base your inferences on the domain name and any
        description the user provided.

        Always respond with valid JSON in the specified format.
        """;

    public AutonomousScopeInferrer(@Qualifier("researchChatModel") ChatLanguageModel model) {
        this.model = model;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Infer scope from domain name and user description.
     */
    public InferredScope inferScope(String domainName, String userDescription, List<String> providedSeeds) {
        log.info("Inferring scope for domain: {}", domainName);

        String prompt = buildPrompt(domainName, userDescription, providedSeeds);
        String response = model.generate(SYSTEM_PROMPT + "\n\n---\n\n" + prompt);

        try {
            return parseResponse(response, domainName, providedSeeds);
        } catch (Exception e) {
            log.error("Failed to parse scope inference response: {}", e.getMessage());
            return InferredScope.minimal(domainName, providedSeeds);
        }
    }

    private String buildPrompt(String domainName, String userDescription, List<String> providedSeeds) {
        StringBuilder sb = new StringBuilder();

        sb.append("## Domain Analysis Request\n\n");
        sb.append("**Domain Name:** ").append(domainName).append("\n\n");

        if (userDescription != null && !userDescription.isBlank()) {
            sb.append("**User Description:** ").append(userDescription).append("\n\n");
        } else {
            sb.append("**User Description:** (not provided - please infer from domain name)\n\n");
        }

        if (providedSeeds != null && !providedSeeds.isEmpty()) {
            sb.append("**User-Provided Seed Topics:** ").append(String.join(", ", providedSeeds)).append("\n\n");
            sb.append("The user has already specified these seed topics. You may suggest additional seeds if appropriate.\n\n");
        }

        sb.append("""
            ## Task

            Analyze the domain and infer appropriate scope boundaries for wiki content generation.

            Respond with JSON in this exact format:
            ```json
            {
              "audienceDescription": "Brief description of target readers",
              "audienceLevel": "beginner|intermediate|advanced|mixed",
              "assumedKnowledge": ["concept 1", "concept 2"],
              "outOfScope": ["excluded topic 1", "excluded topic 2"],
              "focusAreas": ["emphasis 1", "emphasis 2"],
              "suggestedSeeds": ["topic 1", "topic 2", "topic 3"],
              "preferredLanguage": "language or framework if applicable, or null",
              "domainDescription": "Brief 1-2 sentence description of the wiki's purpose",
              "reasoning": "Brief explanation of your scope decisions"
            }
            ```

            Guidelines:
            - assumedKnowledge: What readers should already know (don't teach basics)
            - outOfScope: Topics that are related but should NOT be covered
            - focusAreas: Specific aspects to emphasize (practical, theoretical, etc.)
            - suggestedSeeds: 3-5 foundational topics if user didn't provide seeds
            - Be specific rather than generic
            """);

        return sb.toString();
    }

    private InferredScope parseResponse(String response, String domainName, List<String> providedSeeds) throws Exception {
        JsonNode root = JsonParsingUtils.parseJson(response);

        // Parse audience
        String audienceDescription = JsonParsingUtils.getStringOrDefault(root, "audienceDescription",
                "General technical audience");
        String audienceLevel = JsonParsingUtils.getStringOrDefault(root, "audienceLevel", "intermediate");

        // Parse sets
        Set<String> assumedKnowledge = new HashSet<>(
                JsonParsingUtils.parseStringArray(root, "assumedKnowledge"));
        Set<String> outOfScope = new HashSet<>(
                JsonParsingUtils.parseStringArray(root, "outOfScope"));
        Set<String> focusAreas = new HashSet<>(
                JsonParsingUtils.parseStringArray(root, "focusAreas"));

        // Parse seeds - combine provided and suggested
        List<String> suggestedSeeds = JsonParsingUtils.parseStringArray(root, "suggestedSeeds");
        List<String> allSeeds = new ArrayList<>();
        if (providedSeeds != null && !providedSeeds.isEmpty()) {
            allSeeds.addAll(providedSeeds);
        }
        for (String seed : suggestedSeeds) {
            if (!allSeeds.contains(seed)) {
                allSeeds.add(seed);
            }
        }
        // Ensure at least some seeds
        if (allSeeds.isEmpty()) {
            allSeeds.add(domainName + " Overview");
            allSeeds.add(domainName + " Fundamentals");
        }

        // Parse optional fields
        String preferredLanguage = JsonParsingUtils.getStringOrDefault(root, "preferredLanguage", null);
        String domainDescription = JsonParsingUtils.getStringOrDefault(root, "domainDescription",
                "A comprehensive guide to " + domainName);
        String reasoning = JsonParsingUtils.getStringOrDefault(root, "reasoning", "");

        // Build scope configuration
        ScopeConfiguration scope = ScopeConfiguration.builder()
                .assumedKnowledge(assumedKnowledge)
                .outOfScope(outOfScope)
                .focusAreas(focusAreas)
                .audienceDescription(audienceDescription)
                .domainDescription(domainDescription)
                .preferredLanguage(preferredLanguage)
                .build();

        return new InferredScope(scope, allSeeds, audienceDescription, reasoning);
    }

    /**
     * Result of scope inference.
     */
    public record InferredScope(
            ScopeConfiguration scope,
            List<String> inferredSeeds,
            String audienceProfile,
            String inferenceReasoning
    ) {
        /**
         * Create minimal scope when inference fails.
         */
        public static InferredScope minimal(String domainName, List<String> providedSeeds) {
            List<String> seeds = providedSeeds != null && !providedSeeds.isEmpty()
                    ? providedSeeds
                    : List.of(domainName + " Overview", domainName + " Fundamentals");

            ScopeConfiguration scope = ScopeConfiguration.builder()
                    .domainDescription("A comprehensive guide to " + domainName)
                    .audienceDescription("Technical audience")
                    .build();

            return new InferredScope(scope, seeds, "Technical audience",
                    "Fallback: using minimal scope due to inference failure");
        }
    }
}
