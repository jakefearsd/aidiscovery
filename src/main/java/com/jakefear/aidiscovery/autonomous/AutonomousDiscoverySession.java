package com.jakefear.aidiscovery.autonomous;

import com.jakefear.aidiscovery.discovery.*;
import com.jakefear.aidiscovery.domain.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates fully autonomous topic universe discovery.
 * Replaces human curation with AI decision-making while reusing existing services.
 */
public class AutonomousDiscoverySession {

    private final BufferedReader in;
    private final PrintWriter out;
    private final TopicExpander topicExpander;
    private final RelationshipSuggester relationshipSuggester;
    private final GapAnalyzer gapAnalyzer;
    private final AutonomousConfig config;

    private final AutonomousScopeInferrer scopeInferrer;
    private final AutonomousCurator curator;
    private final AutonomousProgressReporter reporter;

    private DiscoverySession session;
    private int consecutiveLowQualityRounds = 0;

    public AutonomousDiscoverySession(
            BufferedReader in,
            PrintWriter out,
            TopicExpander topicExpander,
            RelationshipSuggester relationshipSuggester,
            GapAnalyzer gapAnalyzer,
            AutonomousConfig config) {

        this.in = in;
        this.out = out;
        this.topicExpander = topicExpander;
        this.relationshipSuggester = relationshipSuggester;
        this.gapAnalyzer = gapAnalyzer;
        this.config = config;

        // Create autonomous components (normally these would be injected)
        this.scopeInferrer = new AutonomousScopeInferrer(
                topicExpander.getModel());
        this.curator = new AutonomousCurator(
                topicExpander.getModel());
        this.reporter = new AutonomousProgressReporter(out, config.verbose(), false);
    }

    /**
     * Run the autonomous discovery process.
     *
     * @return The completed TopicUniverse, or null if cancelled/dry-run
     */
    public TopicUniverse run() {
        try {
            reporter.printBanner(config);

            // Phase 1: Infer scope
            reporter.phaseStarted("Inferring scope from description");
            AutonomousScopeInferrer.InferredScope inferredScope = runScopeInference();

            // Handle confirmation mode
            if (config.confirmBeforeProceeding()) {
                reporter.showInferredScope(config.domainName(), inferredScope.scope(),
                        inferredScope.inferredSeeds());

                out.print("Proceed with autonomous discovery? [Y/n]: ");
                out.flush();

                String response = in.readLine();
                if (response != null && response.trim().toLowerCase().startsWith("n")) {
                    out.println("Cancelled by user.");
                    return null;
                }
            }

            // Initialize session
            initializeSession(inferredScope);

            // Phase 2: Topic expansion
            reporter.phaseStarted("Expanding topics");
            runTopicExpansion();

            // Phase 3: Relationship mapping
            reporter.phaseStarted("Mapping relationships");
            runRelationshipMapping();

            // Phase 4: Gap analysis
            if (!config.costProfile().skipGapAnalysis()) {
                reporter.phaseStarted("Analyzing gaps");
                runGapAnalysis();
            }

            // Phase 5: Prioritization
            reporter.phaseStarted("Prioritizing topics");
            runPrioritization();

            // Phase 6: Finalization
            reporter.phaseStarted("Finalizing universe");
            TopicUniverse universe = session.buildUniverse();

            // Handle dry run
            if (config.dryRun()) {
                reporter.dryRunComplete(universe.getAcceptedCount(),
                        universe.relationships().size());
                return null;
            }

            reporter.printStatistics();
            return universe;

        } catch (Exception e) {
            reporter.error("Autonomous discovery failed", e);
            return null;
        }
    }

    private AutonomousScopeInferrer.InferredScope runScopeInference() {
        return scopeInferrer.inferScope(
                config.domainName(),
                config.userDescription(),
                config.seedTopics()
        );
    }

    private void initializeSession(AutonomousScopeInferrer.InferredScope inferredScope) {
        session = new DiscoverySession(config.domainName());

        // Set domain description
        if (inferredScope.scope().domainDescription() != null) {
            session.setDomainDescription(inferredScope.scope().domainDescription());
        }

        // Configure scope
        session.configureScope(inferredScope.scope());

        // Add landing page
        session.addLandingPage(config.domainName(),
                "Overview of " + config.domainName());

        // Add seed topics
        for (String seed : inferredScope.inferredSeeds()) {
            if (!seed.equals(config.domainName())) {
                session.addSeedTopic(seed, "Seed topic: " + seed);
            }
        }

        // Build domain context
        DomainContext domainContext = topicExpander.buildDomainContext(
                config.domainName(),
                config.userDescription()
        );
        session.setDomainContext(domainContext);

        // Advance to expansion phase
        session.goToPhase(DiscoveryPhase.TOPIC_EXPANSION);
    }

    private void runTopicExpansion() {
        StoppingCriteria criteria = config.stoppingCriteria();
        int round = 0;

        while (!shouldStop(round, criteria)) {
            round++;

            int topicsBeforeRound = session.getAcceptedTopicCount();

            // Get topics to expand (limit by cost profile)
            List<Topic> topicsToExpand = getTopicsToExpand();

            if (topicsToExpand.isEmpty()) {
                reporter.convergenceReached("No more topics to expand");
                break;
            }

            // Expand each topic
            int highQualityCount = 0;
            int acceptedThisRound = 0;

            for (Topic topic : topicsToExpand) {
                List<TopicSuggestion> suggestions = topicExpander.expandTopicWithSearch(
                        topic.name(),
                        config.domainName(),
                        getAcceptedTopicNames(),
                        session.getScope(),
                        config.costProfile()
                );

                // Curate suggestions
                AutonomousContext context = buildContext(round);
                List<AutonomousCurator.CurationDecision> decisions =
                        curator.curateTopicBatch(suggestions, context, config.confidenceThreshold());

                // Apply decisions
                for (int i = 0; i < suggestions.size(); i++) {
                    TopicSuggestion suggestion = suggestions.get(i);
                    AutonomousCurator.CurationDecision decision = decisions.get(i);

                    if (suggestion.getAutonomousQualityScore() >= config.confidenceThreshold()) {
                        highQualityCount++;
                    }

                    if (decision.isAccepted()) {
                        session.acceptTopicSuggestion(suggestion);
                        reporter.topicAccepted(suggestion, decision.reasoning());
                        acceptedThisRound++;
                    } else if (decision.isRejected()) {
                        session.rejectTopicSuggestion(suggestion);
                        reporter.topicRejected(suggestion, decision.reasoning());
                    } else {
                        session.deferTopicSuggestion(suggestion);
                        reporter.topicDeferred(suggestion, decision.reasoning());
                    }

                    // Check capacity
                    if (context.hasMaximumTopics()) {
                        reporter.convergenceReached("Maximum topic count reached");
                        break;
                    }
                }
            }

            reporter.expansionRoundComplete(round, criteria.maxExpansionRounds(),
                    acceptedThisRound, session.getAcceptedTopicCount());

            // Check convergence
            if (criteria.hasConverged(highQualityCount, topicsToExpand.size() *
                    config.costProfile().suggestionsPerTopic())) {
                consecutiveLowQualityRounds++;
                if (criteria.shouldStopByLowQuality(consecutiveLowQualityRounds)) {
                    reporter.convergenceReached("Multiple low-quality rounds");
                    break;
                }
            } else {
                consecutiveLowQualityRounds = 0;
            }
        }
    }

    private boolean shouldStop(int currentRound, StoppingCriteria criteria) {
        // Hard limits
        if (criteria.shouldStopByRounds(currentRound)) return true;
        if (criteria.shouldStopByCount(session.getAcceptedTopicCount())) return true;

        // Minimum not met - continue
        if (!criteria.hasMinimumTopics(session.getAcceptedTopicCount())) return false;

        // Low quality convergence
        return criteria.shouldStopByLowQuality(consecutiveLowQualityRounds);
    }

    private List<Topic> getTopicsToExpand() {
        List<Topic> acceptedTopics = session.buildUniverse().getAcceptedTopics();

        // Get topics that haven't been expanded yet (no relationships from them)
        Set<String> expandedTopics = session.buildUniverse().relationships().stream()
                .map(TopicRelationship::sourceTopicId)
                .collect(Collectors.toSet());

        return acceptedTopics.stream()
                .filter(t -> !expandedTopics.contains(t.id()))
                .limit(config.costProfile().topicsPerRound())
                .toList();
    }

    private Set<String> getAcceptedTopicNames() {
        return session.buildUniverse().getAcceptedTopics().stream()
                .map(Topic::name)
                .collect(Collectors.toSet());
    }

    private void runRelationshipMapping() {
        List<Topic> acceptedTopics = session.buildUniverse().getAcceptedTopics();

        List<RelationshipSuggestion> suggestions = relationshipSuggester.analyzeAllRelationships(
                acceptedTopics
        );

        int autoConfirmed = 0;
        int aiVerified = 0;

        AutonomousContext context = buildContext(0);

        for (RelationshipSuggestion suggestion : suggestions) {
            AutonomousCurator.CurationDecision decision =
                    curator.curateRelationship(suggestion, context);

            if (decision.isAccepted()) {
                session.confirmRelationship(suggestion);
                if (suggestion.confidence() >= 0.8) {
                    autoConfirmed++;
                } else {
                    aiVerified++;
                }
            } else {
                session.rejectRelationship(suggestion);
            }
        }

        reporter.relationshipsConfirmed(autoConfirmed, aiVerified, autoConfirmed + aiVerified);
    }

    private void runGapAnalysis() {
        List<Topic> acceptedTopics = session.buildUniverse().getAcceptedTopics();
        List<TopicRelationship> relationships = session.buildUniverse().relationships();

        GapAnalyzer.GapAnalysisResult result = gapAnalyzer.analyzeGaps(
                acceptedTopics,
                relationships,
                session.getScope()
        );

        int critical = 0, moderate = 0, minor = 0, addressed = 0;

        for (GapAnalyzer.Gap gap : result.gaps()) {
            switch (gap.severity()) {
                case CRITICAL -> {
                    critical++;
                    // Auto-address critical gaps if they have a suggested topic
                    if (gap.suggestedTopicName() != null) {
                        Topic newTopic = Topic.builder(gap.suggestedTopicName())
                                .description(gap.description())
                                .status(TopicStatus.ACCEPTED)
                                .priority(Priority.SHOULD_HAVE)
                                .addedReason("Gap analysis: " + gap.type())
                                .build();
                        session.addressGapWithTopic(gap.description(), newTopic);
                        addressed++;
                    }
                }
                case MODERATE -> {
                    moderate++;
                    // Track moderate gaps but don't auto-address
                    session.addGaps(List.of(gap.description()));
                }
                case MINOR -> {
                    minor++;
                    // Ignore minor gaps
                    session.ignoreGap(gap.description());
                }
            }
        }

        reporter.gapsAnalyzed(critical, moderate, minor, addressed);
    }

    private void runPrioritization() {
        List<Topic> acceptedTopics = session.buildUniverse().getAcceptedTopics();
        List<TopicRelationship> relationships = session.buildUniverse().relationships();

        // Count how many topics depend on each topic (prerequisite count)
        Map<String, Long> prerequisiteOf = relationships.stream()
                .filter(r -> r.type() == RelationshipType.PREREQUISITE_OF)
                .collect(Collectors.groupingBy(TopicRelationship::sourceTopicId, Collectors.counting()));

        for (Topic topic : acceptedTopics) {
            Priority newPriority;

            if (topic.isLandingPage()) {
                newPriority = Priority.MUST_HAVE;
            } else if (prerequisiteOf.getOrDefault(topic.id(), 0L) >= 3) {
                // Prerequisite to many topics
                newPriority = Priority.MUST_HAVE;
            } else if (config.seedTopics().contains(topic.name())) {
                // User-provided seed
                newPriority = Priority.MUST_HAVE;
            } else if (topic.priority() == Priority.MUST_HAVE) {
                // Keep existing MUST_HAVE (likely seeds)
                newPriority = Priority.MUST_HAVE;
            } else {
                // Default to SHOULD_HAVE
                newPriority = Priority.SHOULD_HAVE;
            }

            if (newPriority != topic.priority()) {
                session.updateTopicPriority(topic.id(), newPriority);
            }
        }
    }

    private AutonomousContext buildContext(int round) {
        Set<String> accepted = getAcceptedTopicNames();
        Set<String> rejected = new HashSet<>(); // Could track these if needed
        Set<String> deferred = new HashSet<>();

        StoppingCriteria criteria = config.stoppingCriteria();

        return new AutonomousContext(
                config.domainName(),
                config.userDescription(),
                session.getScope(),
                session.getDomainContext(),
                accepted,
                rejected,
                deferred,
                session.buildUniverse().relationships(),
                config.costProfile(),
                session.getAcceptedTopicCount(),
                criteria.minTopics(),
                criteria.maxTopics(),
                session.getCurrentPhase().name(),
                round,
                criteria.maxExpansionRounds()
        );
    }
}
