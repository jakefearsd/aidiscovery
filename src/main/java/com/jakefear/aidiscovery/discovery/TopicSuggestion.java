package com.jakefear.aidiscovery.discovery;

import com.jakefear.aidiscovery.content.ContentType;
import com.jakefear.aidiscovery.domain.ComplexityLevel;

import java.util.Objects;

import static com.jakefear.aidiscovery.discovery.ScoringConstants.*;

/**
 * A suggested topic from the AI discovery process.
 * Contains the AI's analysis and recommendations before user curation.
 */
public record TopicSuggestion(
        String name,
        String description,
        String category,
        ContentType suggestedContentType,
        ComplexityLevel suggestedComplexity,
        int suggestedWordCount,
        double relevanceScore,
        String rationale,
        String sourceContext,
        double searchConfidence
) {
    /**
     * Compact constructor with validation.
     */
    public TopicSuggestion {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }

        // Defaults
        if (description == null) description = "";
        if (category == null) category = "";
        if (suggestedContentType == null) suggestedContentType = ContentType.CONCEPT;
        if (suggestedComplexity == null) suggestedComplexity = ComplexityLevel.INTERMEDIATE;
        if (suggestedWordCount <= 0) suggestedWordCount = suggestedComplexity.getMinWords();
        if (relevanceScore < 0 || relevanceScore > 1) relevanceScore = DEFAULT_CONFIDENCE;
        if (rationale == null) rationale = "";
        if (sourceContext == null) sourceContext = "";
        if (searchConfidence < 0 || searchConfidence > 1) searchConfidence = NOT_VALIDATED;
    }

    /**
     * Create a simple suggestion with minimal info.
     */
    public static TopicSuggestion simple(String name, String description) {
        return new TopicSuggestion(
                name, description, "", ContentType.CONCEPT,
                ComplexityLevel.INTERMEDIATE, 1000, DEFAULT_CONFIDENCE, "", "", NOT_VALIDATED
        );
    }

    /**
     * Create a suggestion with full AI analysis.
     */
    public static TopicSuggestion analyzed(
            String name,
            String description,
            String category,
            ContentType contentType,
            ComplexityLevel complexity,
            double relevance,
            String rationale) {
        return new TopicSuggestion(
                name, description, category, contentType, complexity,
                complexity.getMinWords(), relevance, rationale, "", NOT_VALIDATED
        );
    }

    /**
     * Create a copy of this suggestion with a specific search confidence.
     */
    public TopicSuggestion withSearchConfidence(double confidence) {
        return new TopicSuggestion(
                name, description, category, suggestedContentType,
                suggestedComplexity, suggestedWordCount, relevanceScore,
                rationale, sourceContext, confidence
        );
    }

    /**
     * Check if this suggestion has a search confidence score (has been validated against search).
     * @return true if searchConfidence >= 0 (not the sentinel value)
     */
    public boolean hasSearchConfidence() {
        return searchConfidence >= 0;
    }

    /**
     * Get quality score for autonomous curation decisions.
     * Weighs relevance and search confidence to produce a single quality score.
     * <p>
     * Formula: (relevance * {@value #RELEVANCE_WEIGHT}) + (searchConfidence * {@value #SEARCH_CONFIDENCE_WEIGHT})
     * For non-validated topics: relevance * {@value #NON_VALIDATED_PENALTY}
     *
     * @return Score from 0.0 to 1.0
     */
    public double getAutonomousQualityScore() {
        if (searchConfidence < 0) {
            // Not search-validated: penalize but still consider relevance
            return relevanceScore * NON_VALIDATED_PENALTY;
        }
        // Weighted combination: relevance matters more than search confirmation
        return (relevanceScore * RELEVANCE_WEIGHT) + (searchConfidence * SEARCH_CONFIDENCE_WEIGHT);
    }

    /**
     * Check if this suggestion meets the threshold for autonomous acceptance.
     *
     * @param threshold Minimum quality score (typically 0.75)
     * @return true if suggestion should be auto-accepted
     */
    public boolean meetsAutonomousThreshold(double threshold) {
        return getAutonomousQualityScore() >= threshold && searchConfidence >= ACCEPT_SEARCH_THRESHOLD;
    }

    /**
     * Check if this suggestion is a candidate for automatic rejection (likely low quality or hallucinated).
     *
     * @return true if suggestion should be auto-rejected
     */
    public boolean isAutoRejectCandidate() {
        return getAutonomousQualityScore() < AUTO_REJECT_SCORE_THRESHOLD
                || (hasSearchConfidence() && searchConfidence < AUTO_REJECT_SEARCH_THRESHOLD);
    }

    /**
     * Get a confidence indicator for display.
     */
    public String getConfidenceIndicator() {
        if (searchConfidence < 0) return "⚪ Not validated";
        if (searchConfidence >= HIGH_CONFIDENCE_THRESHOLD) return "🟢 High confidence";
        if (searchConfidence >= MEDIUM_CONFIDENCE_THRESHOLD) return "🟡 Medium confidence";
        if (searchConfidence >= LOW_CONFIDENCE_THRESHOLD) return "🟠 Low confidence";
        return "🔴 Not found in search";
    }

    /**
     * Create a builder for this suggestion.
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Get a relevance indicator for display.
     */
    public String getRelevanceIndicator() {
        int bars = (int) (relevanceScore * 10);
        return "█".repeat(bars) + "░".repeat(10 - bars);
    }

    /**
     * Get a short summary for display.
     */
    public String getSummary() {
        return String.format("%s (%s, %s, ~%d words)",
                name,
                suggestedContentType.getDisplayName(),
                suggestedComplexity.getDisplayName(),
                suggestedWordCount);
    }

    /**
     * Builder for TopicSuggestion.
     */
    public static class Builder {
        private final String name;
        private String description = "";
        private String category = "";
        private ContentType suggestedContentType = ContentType.CONCEPT;
        private ComplexityLevel suggestedComplexity = ComplexityLevel.INTERMEDIATE;
        private int suggestedWordCount = 1000;
        private double relevanceScore = DEFAULT_CONFIDENCE;
        private String rationale = "";
        private String sourceContext = "";
        private double searchConfidence = NOT_VALIDATED;

        public Builder(String name) {
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder contentType(ContentType contentType) {
            this.suggestedContentType = contentType;
            return this;
        }

        public Builder complexity(ComplexityLevel complexity) {
            this.suggestedComplexity = complexity;
            this.suggestedWordCount = complexity.getMinWords();
            return this;
        }

        public Builder wordCount(int wordCount) {
            this.suggestedWordCount = wordCount;
            return this;
        }

        public Builder relevance(double relevance) {
            this.relevanceScore = relevance;
            return this;
        }

        public Builder rationale(String rationale) {
            this.rationale = rationale;
            return this;
        }

        public Builder sourceContext(String context) {
            this.sourceContext = context;
            return this;
        }

        public Builder searchConfidence(double confidence) {
            this.searchConfidence = confidence;
            return this;
        }

        public TopicSuggestion build() {
            return new TopicSuggestion(
                    name, description, category, suggestedContentType,
                    suggestedComplexity, suggestedWordCount, relevanceScore,
                    rationale, sourceContext, searchConfidence
            );
        }
    }
}
