package com.jakefear.aidiscovery.autonomous;

import com.jakefear.aidiscovery.discovery.TopicSuggestion;
import com.jakefear.aidiscovery.domain.ScopeConfiguration;
import com.jakefear.aidiscovery.domain.TopicUniverse;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;

/**
 * Reports progress during autonomous discovery.
 * Supports verbose and quiet modes.
 */
public class AutonomousProgressReporter {

    private static final int TOTAL_PHASES = 6;

    private final PrintWriter out;
    private final boolean verbose;
    private final boolean quiet;

    private int currentPhase = 0;
    private int acceptedCount = 0;
    private int rejectedCount = 0;

    public AutonomousProgressReporter(PrintWriter out, boolean verbose, boolean quiet) {
        this.out = out;
        this.verbose = verbose;
        this.quiet = quiet;
    }

    /**
     * Print the startup banner.
     */
    public void printBanner(AutonomousConfig config) {
        if (quiet) return;

        out.println();
        out.println("╔═══════════════════════════════════════════════════════════════════╗");
        out.println("║              AI DISCOVERY - AUTONOMOUS MODE                       ║");
        out.println("║                    \"I Feel Lucky\"                                 ║");
        out.println("╚═══════════════════════════════════════════════════════════════════╝");
        out.println();
        out.printf("  Domain:       %s%n", config.domainName());
        if (config.hasDescription()) {
            out.printf("  Description:  %s%n", truncate(config.userDescription(), 50));
        }
        if (config.hasSeedTopics()) {
            out.printf("  Seeds:        %s%n", String.join(", ", config.seedTopics()));
        }
        out.printf("  Cost Profile: %s%n", config.costProfile().name());
        out.printf("  Confidence:   %.0f%%%n", config.confidenceThreshold() * 100);
        out.println();
        out.println("─".repeat(67));
        out.println();
    }

    /**
     * Report phase start.
     */
    public void phaseStarted(String phaseName) {
        currentPhase++;
        if (quiet) return;

        out.printf("[%d/%d] %s...%n", currentPhase, TOTAL_PHASES, phaseName);
    }

    /**
     * Report inferred scope for confirmation mode.
     */
    public void showInferredScope(String domainName, ScopeConfiguration scope, List<String> inferredSeeds) {
        out.println();
        out.println("═══════════════════════════════════════════════════════════════════");
        out.println("                   AUTONOMOUS DISCOVERY PLAN");
        out.println("═══════════════════════════════════════════════════════════════════");
        out.println();
        out.printf("Domain: %s%n", domainName);
        out.println();

        out.println("Inferred Scope:");
        if (scope.audienceDescription() != null && !scope.audienceDescription().isBlank()) {
            out.printf("  Audience:    %s%n", scope.audienceDescription());
        }
        if (!scope.assumedKnowledge().isEmpty()) {
            out.printf("  Assumes:     %s%n", String.join(", ", scope.assumedKnowledge()));
        }
        if (!scope.focusAreas().isEmpty()) {
            out.printf("  Focus:       %s%n", String.join(", ", scope.focusAreas()));
        }
        if (!scope.outOfScope().isEmpty()) {
            out.printf("  Excludes:    %s%n", String.join(", ", scope.outOfScope()));
        }
        out.println();

        if (!inferredSeeds.isEmpty()) {
            out.println("Seed Topics:");
            for (int i = 0; i < inferredSeeds.size(); i++) {
                out.printf("  %d. %s%n", i + 1, inferredSeeds.get(i));
            }
            out.println();
        }

        out.println("═══════════════════════════════════════════════════════════════════");
    }

    /**
     * Report topic accepted.
     */
    public void topicAccepted(TopicSuggestion suggestion, String reason) {
        acceptedCount++;
        if (quiet) return;

        String indicator = suggestion.hasSearchConfidence()
                ? String.format("%.2f", suggestion.getAutonomousQualityScore())
                : "unvalidated";

        out.printf("  + %s (%s)%n", suggestion.name(), indicator);

        if (verbose && reason != null && !reason.isBlank()) {
            out.printf("    Reason: %s%n", truncate(reason, 60));
        }
    }

    /**
     * Report topic rejected.
     */
    public void topicRejected(TopicSuggestion suggestion, String reason) {
        rejectedCount++;
        if (quiet) return;

        if (verbose) {
            out.printf("  - %s (rejected: %s)%n", suggestion.name(), truncate(reason, 40));
        }
    }

    /**
     * Report topic deferred for later consideration.
     */
    public void topicDeferred(TopicSuggestion suggestion, String reason) {
        if (quiet || !verbose) return;

        out.printf("  ~ %s (deferred)%n", suggestion.name());
    }

    /**
     * Report expansion round completion.
     */
    public void expansionRoundComplete(int round, int maxRounds, int newTopicsThisRound, int totalTopics) {
        if (quiet) return;

        out.printf("  Round %d/%d complete: +%d topics (total: %d)%n",
                round, maxRounds, newTopicsThisRound, totalTopics);
    }

    /**
     * Report convergence reached (stopping early).
     */
    public void convergenceReached(String reason) {
        if (quiet) return;

        out.println();
        out.printf("  Convergence: %s%n", reason);
    }

    /**
     * Report relationship mapping progress.
     */
    public void relationshipsConfirmed(int autoConfirmed, int aiVerified, int total) {
        if (quiet) return;

        out.printf("  %d relationships confirmed (%d auto, %d AI-verified)%n",
                total, autoConfirmed, aiVerified);
    }

    /**
     * Report gap analysis progress.
     */
    public void gapsAnalyzed(int critical, int moderate, int minor, int addressed) {
        if (quiet) return;

        out.printf("  Gaps found: %d critical, %d moderate, %d minor (%d addressed)%n",
                critical, moderate, minor, addressed);
    }

    /**
     * Report dry run completion.
     */
    public void dryRunComplete(int topicCount, int relationshipCount) {
        out.println();
        out.println("═══════════════════════════════════════════════════════════════════");
        out.println("DRY RUN COMPLETE - No changes saved");
        out.println();
        out.printf("  Would have created: %d topics, %d relationships%n", topicCount, relationshipCount);
        out.println("═══════════════════════════════════════════════════════════════════");
    }

    /**
     * Report final completion.
     */
    public void complete(TopicUniverse universe, Path savedPath) {
        if (quiet) return;

        out.println();
        out.println("═══════════════════════════════════════════════════════════════════");
        out.println("                    AUTONOMOUS DISCOVERY COMPLETE");
        out.println("═══════════════════════════════════════════════════════════════════");
        out.println();
        out.printf("  Domain:        %s%n", universe.name());
        out.printf("  Topics:        %d accepted%n", universe.getAcceptedCount());
        out.printf("  Relationships: %d mapped%n", universe.relationships().size());
        out.printf("  Est. Words:    ~%,d%n", universe.getEstimatedWordCount());
        out.println();
        out.printf("  Saved to: %s%n", savedPath);
        out.println();
        out.println("To generate articles from this universe:");
        out.printf("  aipublisher --universe %s%n", universe.id());
        out.println("═══════════════════════════════════════════════════════════════════");
    }

    /**
     * Report statistics at the end.
     */
    public void printStatistics() {
        if (quiet) return;

        out.println();
        out.printf("Statistics: %d accepted, %d rejected%n", acceptedCount, rejectedCount);
    }

    /**
     * Report an error.
     */
    public void error(String message, Exception e) {
        out.println();
        out.println("ERROR: " + message);
        if (verbose && e != null) {
            e.printStackTrace(out);
        }
    }

    /**
     * Report a warning.
     */
    public void warn(String message) {
        if (quiet) return;
        out.println("  WARNING: " + message);
    }

    /**
     * Verbose-only message.
     */
    public void debug(String message) {
        if (!verbose) return;
        out.println("  [DEBUG] " + message);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
