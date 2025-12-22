package com.jakefear.aidiscovery.cli;

import com.jakefear.aidiscovery.discovery.CostProfile;
import com.jakefear.aidiscovery.discovery.GapAnalyzer;
import com.jakefear.aidiscovery.discovery.RelationshipSuggester;
import com.jakefear.aidiscovery.discovery.TopicExpander;
import com.jakefear.aidiscovery.domain.TopicUniverse;
import com.jakefear.aidiscovery.domain.TopicUniverseRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Main CLI command for AI Discovery.
 *
 * Provides a command-line interface for interactive domain discovery,
 * building comprehensive topic universes with AI assistance.
 */
@Component
@Command(
        name = "aidiscovery",
        mixinStandardHelpOptions = true,
        version = "AI Discovery 0.1.0-SNAPSHOT",
        description = "Build comprehensive topic universes for wiki content with AI assistance.",
        usageHelpWidth = 120,
        footer = {
                "",
                "Examples:",
                "  aidiscovery                                     # Interactive discovery",
                "  aidiscovery --cost-profile minimal              # Quick prototype mode",
                "  aidiscovery -c balanced                         # Standard coverage",
                "  aidiscovery -c comprehensive                    # Full enterprise coverage",
                "",
                "List and Show Universes:",
                "  aidiscovery --list                              # List all saved universes",
                "  aidiscovery --show my-wiki-id                   # Show universe details",
                "  aidiscovery --export my-wiki-id ./universe.json # Export universe to file",
                "",
                "Cost Profiles:",
                "  MINIMAL       Quick prototype, 2-4 topics, ~$0.50-2",
                "  BALANCED      Good coverage, 9-31 topics, ~$2-5 (default)",
                "  COMPREHENSIVE Full coverage, 25-150 topics, ~$5-15",
                "",
                "LLM Provider Options (Spring Boot properties):",
                "  --llm.provider=<provider>       LLM provider: \"anthropic\" or \"ollama\" (default: anthropic)",
                "  --ollama.base-url=<url>         Ollama server URL (default: http://localhost:11434)",
                "  --ollama.model=<model>          Ollama model name (default: qwen2.5:14b)",
                "  --anthropic.model=<model>       Anthropic model (default: claude-sonnet-4-20250514)",
                "",
                "Using Ollama (local inference - free):",
                "  aidiscovery --llm.provider=ollama",
                "  aidiscovery --llm.provider=ollama --ollama.model=llama3.2",
                "",
                "API Key (required for Anthropic, not needed for Ollama):",
                "  -k, --key         Pass API key directly on command line",
                "  --key-file        Read API key from a file",
                "  ANTHROPIC_API_KEY Environment variable",
                "",
                "Output:",
                "  Discovered universes are saved to ~/.aipublisher/universes/",
                "  Use aipublisher --universe <id> to generate articles from a universe"
        }
)
public class AiDiscoveryCommand implements Callable<Integer> {

    private Supplier<TopicExpander> topicExpanderSupplier;
    private Supplier<RelationshipSuggester> relationshipSuggesterSupplier;
    private Supplier<GapAnalyzer> gapAnalyzerSupplier;
    private Supplier<TopicUniverseRepository> universeRepositorySupplier;

    @Option(names = {"--cost-profile", "-c"},
            description = "Cost profile: MINIMAL (quick prototype), BALANCED (most projects), COMPREHENSIVE (enterprise docs)")
    private String costProfileStr;

    @Option(names = {"--list"},
            description = "List all saved topic universes")
    private boolean listUniverses;

    @Option(names = {"--show"},
            description = "Show details of a saved universe by ID")
    private String showUniverseId;

    @Option(names = {"--export"},
            arity = "2",
            description = "Export universe to a file (usage: --export <id> <path>)")
    private String[] exportArgs;

    @Option(names = {"-v", "--verbose"},
            description = "Enable verbose output")
    private boolean verbose;

    @Option(names = {"-q", "--quiet"},
            description = "Suppress non-essential output")
    private boolean quiet;

    @Option(names = {"-k", "--key"},
            description = "Anthropic API key (overrides environment variable)")
    private String apiKey;

    @Option(names = {"--key-file"},
            description = "Path to file containing Anthropic API key")
    private Path keyFile;

    /**
     * Capture Spring Boot properties (--property.name=value) that picocli doesn't recognize.
     */
    @Unmatched
    private List<String> unmatchedOptions;

    // For testing - allows injecting custom streams
    private BufferedReader inputReader;
    private PrintWriter outputWriter;

    /**
     * Default constructor for Picocli/Spring integration.
     */
    public AiDiscoveryCommand() {
        // Dependencies will be injected via setters
    }

    /**
     * Set the topic expander supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setTopicExpanderProvider(ObjectProvider<TopicExpander> topicExpanderProvider) {
        this.topicExpanderSupplier = topicExpanderProvider::getObject;
    }

    /**
     * Set the relationship suggester supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setRelationshipSuggesterProvider(ObjectProvider<RelationshipSuggester> relationshipSuggesterProvider) {
        this.relationshipSuggesterSupplier = relationshipSuggesterProvider::getObject;
    }

    /**
     * Set the gap analyzer supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setGapAnalyzerProvider(ObjectProvider<GapAnalyzer> gapAnalyzerProvider) {
        this.gapAnalyzerSupplier = gapAnalyzerProvider::getObject;
    }

    /**
     * Set the universe repository supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setUniverseRepositoryProvider(ObjectProvider<TopicUniverseRepository> universeRepositoryProvider) {
        this.universeRepositorySupplier = universeRepositoryProvider::getObject;
    }

    /**
     * Constructor for testing - uses direct instances.
     */
    public AiDiscoveryCommand(TopicExpander topicExpander,
                               RelationshipSuggester relationshipSuggester,
                               GapAnalyzer gapAnalyzer,
                               TopicUniverseRepository universeRepository) {
        this.topicExpanderSupplier = () -> topicExpander;
        this.relationshipSuggesterSupplier = () -> relationshipSuggester;
        this.gapAnalyzerSupplier = () -> gapAnalyzer;
        this.universeRepositorySupplier = () -> universeRepository;
    }

    /**
     * Set custom input/output streams for testing.
     */
    public void setStreams(BufferedReader reader, PrintWriter writer) {
        this.inputReader = reader;
        this.outputWriter = writer;
    }

    @Override
    public Integer call() {
        PrintWriter out = outputWriter != null ? outputWriter : new PrintWriter(System.out, true);
        BufferedReader in = inputReader != null ? inputReader : new BufferedReader(new InputStreamReader(System.in));

        try {
            // Configure API key if provided via CLI
            if (!configureApiKey(out)) {
                return 1;
            }

            // Handle list universes
            if (listUniverses) {
                return listSavedUniverses(out);
            }

            // Handle show universe
            if (showUniverseId != null && !showUniverseId.isBlank()) {
                return showUniverse(out);
            }

            // Handle export universe
            if (exportArgs != null && exportArgs.length == 2) {
                return exportUniverse(out);
            }

            // Default: run interactive discovery session
            return runDiscoveryMode(in, out);

        } catch (Exception e) {
            out.println();
            out.println("ERROR: " + e.getMessage());
            if (verbose) {
                e.printStackTrace(out);
            }
            return 1;
        }
    }

    /**
     * List all saved topic universes.
     */
    private Integer listSavedUniverses(PrintWriter out) {
        TopicUniverseRepository repository = universeRepositorySupplier.get();
        List<String> universes = repository.listAll();

        if (universes.isEmpty()) {
            out.println("No saved universes found.");
            out.println();
            out.println("Create one with: aidiscovery");
            return 0;
        }

        out.println("Saved topic universes:");
        out.println();
        for (String id : universes) {
            repository.load(id).ifPresent(universe -> {
                out.printf("  %s%n", id);
                out.printf("    Name:   %s%n", universe.name());
                out.printf("    Topics: %d%n", universe.getAcceptedCount());
                out.println();
            });
        }

        out.println("To generate articles: aipublisher --universe <id>");
        return 0;
    }

    /**
     * Show details of a saved universe.
     */
    private Integer showUniverse(PrintWriter out) {
        TopicUniverseRepository repository = universeRepositorySupplier.get();

        var universeOpt = repository.load(showUniverseId);
        if (universeOpt.isEmpty()) {
            out.println("Universe not found: " + showUniverseId);
            return 1;
        }

        TopicUniverse universe = universeOpt.get();

        out.println();
        out.println("╔═══════════════════════════════════════════════════════════════════╗");
        out.println("║                        TOPIC UNIVERSE                             ║");
        out.println("╚═══════════════════════════════════════════════════════════════════╝");
        out.println();
        out.printf("  ID:            %s%n", universe.id());
        out.printf("  Name:          %s%n", universe.name());
        out.printf("  Description:   %s%n", universe.description() != null ? universe.description() : "(none)");
        out.printf("  Topics:        %d accepted%n", universe.getAcceptedCount());
        out.printf("  Relationships: %d mapped%n", universe.relationships().size());
        out.printf("  Backlog:       %d items%n", universe.backlog().size());
        out.println();

        out.println("Topics:");
        var order = universe.getGenerationOrder();
        for (int i = 0; i < order.size(); i++) {
            var topic = order.get(i);
            out.printf("  %2d. %s [%s]%n", i + 1, topic.name(), topic.priority().getDisplayName());
        }
        out.println();

        return 0;
    }

    /**
     * Export a universe to a file.
     */
    private Integer exportUniverse(PrintWriter out) {
        String universeId = exportArgs[0];
        Path exportPath = Path.of(exportArgs[1]);

        TopicUniverseRepository repository = universeRepositorySupplier.get();

        var universeOpt = repository.load(universeId);
        if (universeOpt.isEmpty()) {
            out.println("Universe not found: " + universeId);
            return 1;
        }

        try {
            Path savedPath = repository.saveToPath(universeOpt.get(), exportPath);
            out.printf("Exported universe to: %s%n", savedPath);
            return 0;
        } catch (Exception e) {
            out.println("Failed to export: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Run the interactive domain discovery session.
     */
    private Integer runDiscoveryMode(BufferedReader in, PrintWriter out) {
        try {
            // Parse cost profile from CLI if provided
            CostProfile costProfile = null;
            if (costProfileStr != null && !costProfileStr.isBlank()) {
                costProfile = CostProfile.fromName(costProfileStr);
                if (costProfile == null) {
                    out.println("WARNING: Unrecognized cost profile '" + costProfileStr + "'. Will prompt for selection.");
                }
            }

            DiscoveryInteractiveSession discoverySession = new DiscoveryInteractiveSession(
                    in,
                    out,
                    topicExpanderSupplier.get(),
                    relationshipSuggesterSupplier.get(),
                    gapAnalyzerSupplier.get(),
                    costProfile
            );

            TopicUniverse universe = discoverySession.run();

            if (universe == null) {
                // User cancelled
                return 0;
            }

            // Save the universe
            TopicUniverseRepository repository = universeRepositorySupplier.get();
            Path savedPath = repository.save(universe);

            out.println();
            out.println("═".repeat(67));
            out.println("Topic universe saved!");
            out.println();
            out.printf("  ID:       %s%n", universe.id());
            out.printf("  Name:     %s%n", universe.name());
            out.printf("  Topics:   %d accepted%n", universe.getAcceptedCount());
            out.printf("  Location: %s%n", savedPath);
            out.println();
            out.println("To generate articles from this universe, use:");
            out.printf("  aipublisher --universe %s%n", universe.id());
            out.println("═".repeat(67));

            return 0;

        } catch (Exception e) {
            out.println();
            out.println("ERROR in discovery mode: " + e.getMessage());
            if (verbose) {
                e.printStackTrace(out);
            }
            return 1;
        }
    }

    /**
     * Configure the API key from CLI options.
     * Priority: --key > --key-file > ANTHROPIC_API_KEY env var
     *
     * @return true if API key is configured, false on error
     */
    private boolean configureApiKey(PrintWriter out) {
        String key = null;

        // Priority 1: Direct key from command line
        if (apiKey != null && !apiKey.isBlank()) {
            key = apiKey.trim();
            if (verbose) {
                out.println("Using API key from --key option");
            }
        }
        // Priority 2: Key file
        else if (keyFile != null) {
            try {
                if (!Files.exists(keyFile)) {
                    out.println("ERROR: Key file not found: " + keyFile);
                    return false;
                }
                key = Files.readString(keyFile).trim();
                if (key.isBlank()) {
                    out.println("ERROR: Key file is empty: " + keyFile);
                    return false;
                }
                if (verbose) {
                    out.println("Using API key from file: " + keyFile);
                }
            } catch (Exception e) {
                out.println("ERROR: Failed to read key file: " + e.getMessage());
                return false;
            }
        }

        // Set the API key as system property if provided via CLI
        if (key != null) {
            System.setProperty("ANTHROPIC_API_KEY", key);
        }

        return true;
    }

    // Getters for testing
    public String getCostProfileStr() {
        return costProfileStr;
    }

    public boolean isListUniverses() {
        return listUniverses;
    }

    public String getShowUniverseId() {
        return showUniverseId;
    }

    public boolean isVerbose() {
        return verbose;
    }
}
