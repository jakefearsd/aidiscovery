package com.jakefear.aidiscovery.autonomous;

import com.jakefear.aidiscovery.discovery.CostProfile;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for autonomous "I Feel Lucky" discovery mode.
 * Contains all settings needed for fully automated universe generation.
 */
public record AutonomousConfig(
        String domainName,
        String userDescription,
        List<String> seedTopics,
        CostProfile costProfile,
        Path outputPath,
        double confidenceThreshold,
        boolean confirmBeforeProceeding,
        boolean dryRun,
        boolean verbose,
        StoppingCriteria stoppingCriteria
) {
    /**
     * Default confidence threshold for auto-accepting suggestions.
     */
    public static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.75;

    /**
     * Compact constructor with validation.
     */
    public AutonomousConfig {
        Objects.requireNonNull(domainName, "domainName cannot be null");
        if (domainName.isBlank()) {
            throw new IllegalArgumentException("domainName cannot be blank");
        }

        if (userDescription == null) userDescription = "";
        if (seedTopics == null) seedTopics = List.of();
        if (costProfile == null) costProfile = CostProfile.BALANCED;
        if (confidenceThreshold <= 0 || confidenceThreshold > 1) {
            confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD;
        }
        if (stoppingCriteria == null) {
            stoppingCriteria = StoppingCriteria.fromCostProfile(costProfile);
        }
    }

    /**
     * Create a builder for AutonomousConfig.
     */
    public static Builder builder(String domainName) {
        return new Builder(domainName);
    }

    /**
     * Check if user provided a description.
     */
    public boolean hasDescription() {
        return userDescription != null && !userDescription.isBlank();
    }

    /**
     * Check if user provided seed topics.
     */
    public boolean hasSeedTopics() {
        return seedTopics != null && !seedTopics.isEmpty();
    }

    /**
     * Get a display-friendly summary of the configuration.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Domain: ").append(domainName).append("\n");
        if (hasDescription()) {
            sb.append("Description: ").append(userDescription).append("\n");
        }
        if (hasSeedTopics()) {
            sb.append("Seeds: ").append(String.join(", ", seedTopics)).append("\n");
        }
        sb.append("Cost Profile: ").append(costProfile.name()).append("\n");
        sb.append("Confidence Threshold: ").append(String.format("%.0f%%", confidenceThreshold * 100)).append("\n");
        if (confirmBeforeProceeding) {
            sb.append("Mode: Confirm before proceeding\n");
        } else {
            sb.append("Mode: Fully autonomous\n");
        }
        return sb.toString();
    }

    /**
     * Builder for AutonomousConfig.
     */
    public static class Builder {
        private final String domainName;
        private String userDescription = "";
        private List<String> seedTopics = List.of();
        private CostProfile costProfile = CostProfile.BALANCED;
        private Path outputPath = null;
        private double confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD;
        private boolean confirmBeforeProceeding = false;
        private boolean dryRun = false;
        private boolean verbose = false;
        private StoppingCriteria stoppingCriteria = null;

        public Builder(String domainName) {
            this.domainName = domainName;
        }

        public Builder description(String description) {
            this.userDescription = description;
            return this;
        }

        public Builder seeds(List<String> seeds) {
            this.seedTopics = seeds;
            return this;
        }

        public Builder seeds(String... seeds) {
            this.seedTopics = List.of(seeds);
            return this;
        }

        public Builder costProfile(CostProfile profile) {
            this.costProfile = profile;
            return this;
        }

        public Builder outputPath(Path path) {
            this.outputPath = path;
            return this;
        }

        public Builder confidenceThreshold(double threshold) {
            this.confidenceThreshold = threshold;
            return this;
        }

        public Builder confirmBeforeProceeding(boolean confirm) {
            this.confirmBeforeProceeding = confirm;
            return this;
        }

        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder stoppingCriteria(StoppingCriteria criteria) {
            this.stoppingCriteria = criteria;
            return this;
        }

        public AutonomousConfig build() {
            return new AutonomousConfig(
                    domainName,
                    userDescription,
                    seedTopics,
                    costProfile,
                    outputPath,
                    confidenceThreshold,
                    confirmBeforeProceeding,
                    dryRun,
                    verbose,
                    stoppingCriteria
            );
        }
    }
}
