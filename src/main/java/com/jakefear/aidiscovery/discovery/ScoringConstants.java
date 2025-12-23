package com.jakefear.aidiscovery.discovery;

/**
 * Constants for topic and relationship scoring throughout the discovery process.
 * Centralizes all threshold values for autonomous curation decisions and confidence scoring.
 */
public final class ScoringConstants {

    private ScoringConstants() {
        // Utility class - prevent instantiation
    }

    // ==================== Combined Score Weights ====================

    /**
     * Weight applied to relevance score in combined quality calculation.
     * Relevance (0.6) weighs more than search confidence (0.4) because
     * AI-assessed relevance captures semantic fit to the domain.
     */
    public static final double RELEVANCE_WEIGHT = 0.6;

    /**
     * Weight applied to search confidence in combined quality calculation.
     * Search confidence (0.4) provides grounding but matters less than relevance
     * since some valid topics may have limited search presence.
     */
    public static final double SEARCH_CONFIDENCE_WEIGHT = 0.4;

    /**
     * Penalty multiplier for suggestions without search validation.
     * Applied to relevance-only scoring (0.7 = 30% penalty) to discourage
     * potential hallucinations while not completely dismissing them.
     */
    public static final double NON_VALIDATED_PENALTY = 0.7;

    // ==================== Autonomous Decision Thresholds ====================

    /**
     * Minimum search confidence required for autonomous acceptance.
     * Topics below this threshold require AI reasoning even if relevance is high.
     */
    public static final double ACCEPT_SEARCH_THRESHOLD = 0.3;

    /**
     * Combined score below which topics are auto-rejected.
     * Represents clearly low-quality suggestions not worth human review.
     */
    public static final double AUTO_REJECT_SCORE_THRESHOLD = 0.4;

    /**
     * Search confidence below which validated topics are auto-rejected.
     * Topics that searched but found almost nothing are likely hallucinated.
     */
    public static final double AUTO_REJECT_SEARCH_THRESHOLD = 0.2;

    // ==================== Confidence Level Thresholds ====================

    /**
     * Threshold for high confidence (topic or relationship).
     * Above this level, suggestions can be auto-accepted.
     */
    public static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;

    /**
     * Threshold for medium confidence.
     * Between medium and high, suggestions are acceptable but borderline.
     */
    public static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.5;

    /**
     * Threshold for low confidence (display purposes).
     * Between low and medium, suggestions warrant caution.
     */
    public static final double LOW_CONFIDENCE_THRESHOLD = 0.3;

    // ==================== Relationship-Specific Thresholds ====================

    /**
     * Confidence below which relationships are auto-rejected.
     * Lower than topic threshold since relationship detection is noisier.
     */
    public static final double RELATIONSHIP_REJECT_THRESHOLD = 0.4;

    // ==================== Default Values ====================

    /**
     * Default confidence when no value is provided or validation fails.
     */
    public static final double DEFAULT_CONFIDENCE = 0.5;

    /**
     * Sentinel value indicating a topic has not been search-validated.
     */
    public static final double NOT_VALIDATED = -1.0;
}
