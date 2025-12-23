package com.jakefear.aidiscovery.autonomous;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakefear.aidiscovery.cli.curation.CurationAction;
import com.jakefear.aidiscovery.discovery.RelationshipSuggestion;
import com.jakefear.aidiscovery.discovery.TopicSuggestion;
import com.jakefear.aidiscovery.util.JsonParsingUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.jakefear.aidiscovery.discovery.ScoringConstants.*;

/**
 * AI-powered curator that makes autonomous ACCEPT/REJECT/DEFER decisions.
 * Uses existing confidence scores plus additional AI reasoning for borderline cases.
 */
@Component
public class AutonomousCurator {

    private static final Logger log = LoggerFactory.getLogger(AutonomousCurator.class);

    private final ChatLanguageModel model;
    private final ObjectMapper objectMapper;

    private static final String CURATION_SYSTEM_PROMPT = """
        You are a content curator for a technical wiki. Your job is to decide whether
        to include suggested topics in the wiki based on relevance, quality, and scope.

        For each topic, you must decide:
        - ACCEPT: Include this topic in the wiki
        - REJECT: Do not include this topic
        - DEFER: Save for later consideration (borderline cases)

        Consider:
        1. Is this topic relevant to the domain?
        2. Is it within scope (not excluded, matches focus areas)?
        3. Is it distinct from existing topics (not a duplicate)?
        4. Does it add value to the wiki structure?

        Always respond with valid JSON in the specified format.
        """;

    public AutonomousCurator(@Qualifier("curationModel") ChatLanguageModel model) {
        this.model = model;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Curate a single topic suggestion.
     * Uses rules-based logic for clear cases, AI for borderline decisions.
     */
    public CurationDecision curateTopic(TopicSuggestion suggestion, AutonomousContext context,
                                        double confidenceThreshold) {

        // Rule 1: Auto-accept high-quality suggestions
        if (suggestion.meetsAutonomousThreshold(confidenceThreshold)) {
            return CurationDecision.accept("High confidence: score " +
                    String.format("%.2f", suggestion.getAutonomousQualityScore()));
        }

        // Rule 2: Auto-reject clearly low-quality suggestions
        if (suggestion.isAutoRejectCandidate()) {
            return CurationDecision.reject("Low quality: score " +
                    String.format("%.2f", suggestion.getAutonomousQualityScore()) +
                    ", confidence " + String.format("%.2f", suggestion.searchConfidence()));
        }

        // Rule 3: Check for duplicates
        if (context.isAlreadyProcessed(suggestion.name())) {
            return CurationDecision.reject("Duplicate: already processed");
        }

        // Rule 4: Check capacity
        if (context.hasMaximumTopics()) {
            return CurationDecision.reject("Capacity reached: at maximum topic count");
        }

        // Rule 5: Borderline cases - ask AI
        return curateWithAI(suggestion, context);
    }

    /**
     * Curate a batch of topic suggestions efficiently.
     */
    public List<CurationDecision> curateTopicBatch(List<TopicSuggestion> suggestions,
                                                   AutonomousContext context,
                                                   double confidenceThreshold) {
        List<CurationDecision> decisions = new ArrayList<>();
        List<TopicSuggestion> borderlineCases = new ArrayList<>();

        // First pass: apply rules
        for (TopicSuggestion suggestion : suggestions) {
            if (suggestion.meetsAutonomousThreshold(confidenceThreshold)) {
                decisions.add(CurationDecision.accept("High confidence"));
            } else if (suggestion.isAutoRejectCandidate()) {
                decisions.add(CurationDecision.reject("Low quality"));
            } else if (context.isAlreadyProcessed(suggestion.name())) {
                decisions.add(CurationDecision.reject("Duplicate"));
            } else {
                borderlineCases.add(suggestion);
                decisions.add(null); // Placeholder
            }
        }

        // Second pass: batch AI curation for borderline cases
        if (!borderlineCases.isEmpty()) {
            List<CurationDecision> aiDecisions = curateBatchWithAI(borderlineCases, context);

            int aiIndex = 0;
            for (int i = 0; i < decisions.size(); i++) {
                if (decisions.get(i) == null) {
                    decisions.set(i, aiDecisions.get(aiIndex++));
                }
            }
        }

        return decisions;
    }

    /**
     * Curate a relationship suggestion.
     */
    public CurationDecision curateRelationship(RelationshipSuggestion suggestion,
                                               AutonomousContext context) {
        // High confidence relationships are auto-confirmed
        if (suggestion.confidence() >= HIGH_CONFIDENCE_THRESHOLD) {
            return CurationDecision.accept("High confidence: " +
                    String.format("%.2f", suggestion.confidence()));
        }

        // Low confidence relationships are auto-rejected
        if (suggestion.confidence() < RELATIONSHIP_REJECT_THRESHOLD) {
            return CurationDecision.reject("Low confidence: " +
                    String.format("%.2f", suggestion.confidence()));
        }

        // Medium confidence - accept but note it's borderline
        return CurationDecision.accept("Medium confidence: " +
                String.format("%.2f", suggestion.confidence()));
    }

    private CurationDecision curateWithAI(TopicSuggestion suggestion, AutonomousContext context) {
        try {
            String prompt = buildCurationPrompt(suggestion, context);
            String response = model.generate(CURATION_SYSTEM_PROMPT + "\n\n---\n\n" + prompt);
            return parseCurationResponse(response);
        } catch (Exception e) {
            log.warn("AI curation failed for topic '{}': {}", suggestion.name(), e.getMessage());
            // Fallback: defer borderline cases
            return CurationDecision.defer("AI curation failed: " + e.getMessage());
        }
    }

    private List<CurationDecision> curateBatchWithAI(List<TopicSuggestion> suggestions,
                                                     AutonomousContext context) {
        try {
            String prompt = buildBatchCurationPrompt(suggestions, context);
            String response = model.generate(CURATION_SYSTEM_PROMPT + "\n\n---\n\n" + prompt);
            return parseBatchCurationResponse(response, suggestions.size());
        } catch (Exception e) {
            log.warn("Batch AI curation failed: {}", e.getMessage());
            // Fallback: defer all
            return suggestions.stream()
                    .map(s -> CurationDecision.defer("AI curation failed"))
                    .toList();
        }
    }

    private String buildCurationPrompt(TopicSuggestion suggestion, AutonomousContext context) {
        return String.format("""
            ## Topic Curation Request

            %s

            ## Topic to Evaluate

            **Name:** %s
            **Description:** %s
            **Category:** %s
            **Relevance Score:** %.2f
            **Search Confidence:** %.2f
            **Rationale:** %s

            ## Your Decision

            Respond with JSON:
            ```json
            {
              "action": "ACCEPT|REJECT|DEFER",
              "reasoning": "Brief explanation"
            }
            ```
            """,
                context.toPromptFormat(),
                suggestion.name(),
                suggestion.description(),
                suggestion.category(),
                suggestion.relevanceScore(),
                suggestion.searchConfidence(),
                suggestion.rationale()
        );
    }

    private String buildBatchCurationPrompt(List<TopicSuggestion> suggestions, AutonomousContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Batch Topic Curation Request\n\n");
        sb.append(context.toPromptFormat());
        sb.append("\n## Topics to Evaluate\n\n");

        for (int i = 0; i < suggestions.size(); i++) {
            TopicSuggestion s = suggestions.get(i);
            sb.append(String.format("%d. **%s** (relevance: %.2f, search: %.2f)\n   %s\n\n",
                    i + 1, s.name(), s.relevanceScore(), s.searchConfidence(), s.description()));
        }

        sb.append("""
            ## Your Decisions

            Respond with JSON array:
            ```json
            {
              "decisions": [
                {"action": "ACCEPT|REJECT|DEFER", "reasoning": "Brief explanation"},
                ...
              ]
            }
            ```
            """);

        return sb.toString();
    }

    private CurationDecision parseCurationResponse(String response) throws Exception {
        JsonNode root = JsonParsingUtils.parseJson(response);

        String action = JsonParsingUtils.getStringOrDefault(root, "action", "DEFER").toUpperCase();
        String reasoning = JsonParsingUtils.getStringOrDefault(root, "reasoning", "AI decision");

        return decisionFromAction(action, reasoning);
    }

    private List<CurationDecision> parseBatchCurationResponse(String response, int expectedCount)
            throws Exception {
        JsonNode root = JsonParsingUtils.parseJson(response);
        JsonNode decisions = root.get("decisions");

        List<CurationDecision> result = new ArrayList<>();
        if (decisions != null && decisions.isArray()) {
            for (JsonNode decision : decisions) {
                String action = JsonParsingUtils.getStringOrDefault(decision, "action", "DEFER").toUpperCase();
                String reasoning = JsonParsingUtils.getStringOrDefault(decision, "reasoning", "AI decision");
                result.add(decisionFromAction(action, reasoning));
            }
        }

        // Pad with defer if needed
        while (result.size() < expectedCount) {
            result.add(CurationDecision.defer("No decision provided"));
        }

        return result;
    }

    private CurationDecision decisionFromAction(String action, String reasoning) {
        return switch (action) {
            case "ACCEPT" -> CurationDecision.accept(reasoning);
            case "REJECT" -> CurationDecision.reject(reasoning);
            default -> CurationDecision.defer(reasoning);
        };
    }

    /**
     * Result of a curation decision.
     */
    public record CurationDecision(
            CurationAction action,
            String reasoning,
            double confidence,
            Map<String, Object> modifications
    ) {
        public static CurationDecision accept(String reasoning) {
            return new CurationDecision(CurationAction.ACCEPT, reasoning, 1.0, Map.of());
        }

        public static CurationDecision reject(String reasoning) {
            return new CurationDecision(CurationAction.REJECT, reasoning, 1.0, Map.of());
        }

        public static CurationDecision defer(String reasoning) {
            return new CurationDecision(CurationAction.DEFER, reasoning, 0.5, Map.of());
        }

        public static CurationDecision modify(String reasoning, Map<String, Object> modifications) {
            return new CurationDecision(CurationAction.MODIFY, reasoning, 1.0, modifications);
        }

        public boolean isAccepted() {
            return action == CurationAction.ACCEPT;
        }

        public boolean isRejected() {
            return action == CurationAction.REJECT;
        }

        public boolean isDeferred() {
            return action == CurationAction.DEFER;
        }
    }
}
