package com.jakefear.aidiscovery.autonomous;

import com.jakefear.aidiscovery.discovery.CostProfile;

/**
 * Criteria for when autonomous discovery should stop expanding.
 * Provides bounds and convergence detection to prevent runaway API usage.
 */
public record StoppingCriteria(
        int minTopics,
        int maxTopics,
        int maxExpansionRounds,
        int maxConsecutiveLowQualityRounds,
        double convergenceThreshold,
        boolean requireGapSatisfaction
) {
    /**
     * Compact constructor with validation.
     */
    public StoppingCriteria {
        if (minTopics < 1) minTopics = 3;
        if (maxTopics < minTopics) maxTopics = minTopics * 5;
        if (maxExpansionRounds < 1) maxExpansionRounds = 3;
        if (maxConsecutiveLowQualityRounds < 1) maxConsecutiveLowQualityRounds = 3;
        if (convergenceThreshold <= 0 || convergenceThreshold > 1) {
            convergenceThreshold = 0.5;
        }
    }

    /**
     * Create stopping criteria from a cost profile.
     */
    public static StoppingCriteria fromCostProfile(CostProfile profile) {
        return switch (profile.name().toUpperCase()) {
            case "MINIMAL" -> new StoppingCriteria(
                    3,   // minTopics
                    15,  // maxTopics
                    1,   // maxExpansionRounds
                    2,   // maxConsecutiveLowQualityRounds
                    0.6, // convergenceThreshold
                    false // requireGapSatisfaction
            );
            case "COMPREHENSIVE" -> new StoppingCriteria(
                    20,  // minTopics
                    150, // maxTopics
                    5,   // maxExpansionRounds
                    3,   // maxConsecutiveLowQualityRounds
                    0.4, // convergenceThreshold (more lenient)
                    true // requireGapSatisfaction
            );
            default -> new StoppingCriteria( // BALANCED
                    8,   // minTopics
                    40,  // maxTopics
                    3,   // maxExpansionRounds
                    3,   // maxConsecutiveLowQualityRounds
                    0.5, // convergenceThreshold
                    true // requireGapSatisfaction
            );
        };
    }

    /**
     * Check if we should stop based on topic count.
     */
    public boolean shouldStopByCount(int currentTopicCount) {
        return currentTopicCount >= maxTopics;
    }

    /**
     * Check if we have enough topics to consider stopping.
     */
    public boolean hasMinimumTopics(int currentTopicCount) {
        return currentTopicCount >= minTopics;
    }

    /**
     * Check if we should stop due to convergence (no more good suggestions).
     *
     * @param highQualityCount Number of suggestions that met the quality threshold
     * @param totalCount       Total suggestions in the round
     * @return true if quality ratio is below convergence threshold
     */
    public boolean hasConverged(int highQualityCount, int totalCount) {
        if (totalCount == 0) return true;
        double ratio = (double) highQualityCount / totalCount;
        return ratio < convergenceThreshold;
    }

    /**
     * Check if we should stop due to consecutive low-quality rounds.
     */
    public boolean shouldStopByLowQuality(int consecutiveLowQualityRounds) {
        return consecutiveLowQualityRounds >= maxConsecutiveLowQualityRounds;
    }

    /**
     * Check if we should stop due to expansion rounds limit.
     */
    public boolean shouldStopByRounds(int currentRound) {
        return currentRound >= maxExpansionRounds;
    }

    /**
     * Get a human-readable description of stopping behavior.
     */
    public String getDescription() {
        return String.format(
                "Stop when: %d-%d topics, max %d rounds, convergence < %.0f%%",
                minTopics, maxTopics, maxExpansionRounds, convergenceThreshold * 100
        );
    }
}
