package com.jakefear.aidiscovery.autonomous;

import com.jakefear.aidiscovery.discovery.CostProfile;
import com.jakefear.aidiscovery.discovery.DiscoverySession;
import com.jakefear.aidiscovery.domain.DomainContext;
import com.jakefear.aidiscovery.domain.ScopeConfiguration;
import com.jakefear.aidiscovery.domain.TopicRelationship;
import com.jakefear.aidiscovery.domain.TopicUniverse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rich context passed to AI for autonomous decision-making.
 * Contains all information the AI needs to make informed curation decisions.
 */
public record AutonomousContext(
        String domainName,
        String userDescription,
        ScopeConfiguration scope,
        DomainContext domainContext,
        Set<String> acceptedTopicNames,
        Set<String> rejectedTopicNames,
        Set<String> deferredTopicNames,
        List<TopicRelationship> relationships,
        CostProfile costProfile,
        int currentTopicCount,
        int targetMinTopics,
        int targetMaxTopics,
        String currentPhase,
        int currentRound,
        int maxRounds
) {
    /**
     * Create context from a discovery session and autonomous config.
     */
    public static AutonomousContext from(DiscoverySession session, AutonomousConfig config) {
        TopicUniverse universe = session.buildUniverse();

        var accepted = universe.getAcceptedTopics().stream()
                .map(t -> t.name())
                .collect(Collectors.toSet());

        var stoppingCriteria = config.stoppingCriteria();

        return new AutonomousContext(
                session.getDomainName(),
                config.userDescription(),
                session.getScope(),
                session.getDomainContext(),
                accepted,
                Set.of(), // rejected - not tracked
                Set.of(), // deferred - not tracked
                universe.relationships(),
                config.costProfile(),
                session.getAcceptedTopicCount(),
                stoppingCriteria.minTopics(),
                stoppingCriteria.maxTopics(),
                session.getCurrentPhase().name(),
                0, // Will be updated by caller
                stoppingCriteria.maxExpansionRounds()
        );
    }

    /**
     * Create updated context with a new round number.
     */
    public AutonomousContext withRound(int round) {
        return new AutonomousContext(
                domainName, userDescription, scope, domainContext,
                acceptedTopicNames, rejectedTopicNames, deferredTopicNames,
                relationships, costProfile, currentTopicCount,
                targetMinTopics, targetMaxTopics, currentPhase,
                round, maxRounds
        );
    }

    /**
     * Check if we have enough topics to consider stopping.
     */
    public boolean hasMinimumTopics() {
        return currentTopicCount >= targetMinTopics;
    }

    /**
     * Check if we've hit the maximum topic limit.
     */
    public boolean hasMaximumTopics() {
        return currentTopicCount >= targetMaxTopics;
    }

    /**
     * Get remaining topic capacity.
     */
    public int remainingCapacity() {
        return Math.max(0, targetMaxTopics - currentTopicCount);
    }

    /**
     * Check if a topic name has already been processed.
     */
    public boolean isAlreadyProcessed(String topicName) {
        String normalized = topicName.toLowerCase().trim();
        return acceptedTopicNames.stream().anyMatch(n -> n.toLowerCase().equals(normalized))
                || rejectedTopicNames.stream().anyMatch(n -> n.toLowerCase().equals(normalized))
                || deferredTopicNames.stream().anyMatch(n -> n.toLowerCase().equals(normalized));
    }

    /**
     * Format context for inclusion in AI prompts.
     */
    public String toPromptFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Current Discovery Context\n\n");

        sb.append("**Domain:** ").append(domainName).append("\n");
        if (userDescription != null && !userDescription.isBlank()) {
            sb.append("**User Goal:** ").append(userDescription).append("\n");
        }
        sb.append("\n");

        sb.append("**Progress:**\n");
        sb.append("- Current phase: ").append(currentPhase).append("\n");
        sb.append("- Round: ").append(currentRound).append(" of ").append(maxRounds).append("\n");
        sb.append("- Topics accepted: ").append(currentTopicCount)
                .append(" (target: ").append(targetMinTopics).append("-").append(targetMaxTopics).append(")\n");
        sb.append("\n");

        if (!acceptedTopicNames.isEmpty()) {
            sb.append("**Accepted Topics:**\n");
            acceptedTopicNames.stream().sorted().limit(20).forEach(name ->
                    sb.append("- ").append(name).append("\n")
            );
            if (acceptedTopicNames.size() > 20) {
                sb.append("- ... and ").append(acceptedTopicNames.size() - 20).append(" more\n");
            }
            sb.append("\n");
        }

        if (scope != null) {
            sb.append("**Scope:**\n");
            sb.append(scope.toPromptFormat());
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Get a short summary for logging.
     */
    public String getSummary() {
        return String.format("[Round %d/%d] %d topics accepted (target: %d-%d)",
                currentRound, maxRounds, currentTopicCount, targetMinTopics, targetMaxTopics);
    }
}
