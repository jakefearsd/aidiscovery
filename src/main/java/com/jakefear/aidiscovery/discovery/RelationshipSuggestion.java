package com.jakefear.aidiscovery.discovery;

import com.jakefear.aidiscovery.domain.RelationshipType;

import java.util.Objects;

import static com.jakefear.aidiscovery.discovery.ScoringConstants.*;

/**
 * A suggested relationship between topics from the AI discovery process.
 */
public record RelationshipSuggestion(
        String sourceTopicName,
        String targetTopicName,
        RelationshipType suggestedType,
        double confidence,
        String rationale
) {
    /**
     * Compact constructor with validation.
     */
    public RelationshipSuggestion {
        Objects.requireNonNull(sourceTopicName, "sourceTopicName cannot be null");
        Objects.requireNonNull(targetTopicName, "targetTopicName cannot be null");
        Objects.requireNonNull(suggestedType, "suggestedType cannot be null");

        if (sourceTopicName.equals(targetTopicName)) {
            throw new IllegalArgumentException("Cannot create self-referential relationship");
        }

        if (confidence < 0 || confidence > 1) confidence = DEFAULT_CONFIDENCE;
        if (rationale == null) rationale = "";
    }

    /**
     * Create a simple relationship suggestion.
     */
    public static RelationshipSuggestion simple(
            String source,
            String target,
            RelationshipType type) {
        return new RelationshipSuggestion(source, target, type, DEFAULT_CONFIDENCE, "");
    }

    /**
     * Create a relationship suggestion with confidence.
     */
    public static RelationshipSuggestion withConfidence(
            String source,
            String target,
            RelationshipType type,
            double confidence) {
        return new RelationshipSuggestion(source, target, type, confidence, "");
    }

    /**
     * Create a full relationship suggestion.
     */
    public static RelationshipSuggestion full(
            String source,
            String target,
            RelationshipType type,
            double confidence,
            String rationale) {
        return new RelationshipSuggestion(source, target, type, confidence, rationale);
    }

    /**
     * Get a human-readable description of this relationship.
     */
    public String describe() {
        return String.format("\"%s\" %s \"%s\"",
                sourceTopicName,
                suggestedType.getDisplayName().toLowerCase(),
                targetTopicName);
    }

    /**
     * Get a short display format.
     */
    public String toDisplayString() {
        String confidenceIndicator = confidence >= HIGH_CONFIDENCE_THRESHOLD ? "●" :
                confidence >= MEDIUM_CONFIDENCE_THRESHOLD ? "◐" : "○";
        return String.format("%s %s ──[%s]──> %s",
                confidenceIndicator,
                sourceTopicName,
                suggestedType.name(),
                targetTopicName);
    }

    /**
     * Check if this is a high-confidence suggestion.
     */
    public boolean isHighConfidence() {
        return confidence >= HIGH_CONFIDENCE_THRESHOLD;
    }

    /**
     * Check if this relationship implies generation ordering.
     */
    public boolean impliesOrdering() {
        return suggestedType.impliesOrdering();
    }
}
