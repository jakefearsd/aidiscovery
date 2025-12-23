# CLAUDE.md - AI Discovery Development Guidelines

## Project Overview

AI Discovery is an interactive CLI tool for building topic universes through a guided 8-phase workflow. It uses LLM-powered suggestions with human-in-the-loop curation.

**Stats:** ~7,600 LOC, 57 source files, 12 test files, 222 tests

## Core Workflow

```
SEED_INPUT → SCOPE_SETUP → TOPIC_EXPANSION → RELATIONSHIP_MAPPING
     → GAP_ANALYSIS → DEPTH_CALIBRATION → PRIORITIZATION → REVIEW → COMPLETE
```

Each phase is handled by a `PhaseHandler` implementation (State pattern).

## Design Patterns

### Command Pattern
**Location:** `cli/curation/`

User curation actions are encapsulated as commands:
```java
public interface CurationCommand<T> {
    CurationResult execute(T suggestion, DiscoverySession session, ConsoleInputHelper input);
    CurationAction getAction();
}
```

**Implementations:**
- `SimpleCurationCommand<T>` - Generic command using functional delegation for accept/reject/defer/confirm actions
- `ModifyTopicCommand` - Complex command with interactive topic modification logic

```java
// SimpleCurationCommand uses BiConsumer for session action
new SimpleCurationCommand<>(CurationAction.ACCEPT, "Accepted", DiscoverySession::acceptTopicSuggestion);
```

**Factory:** `TopicCurationCommandFactory`, `RelationshipCurationCommandFactory`

### State Pattern
**Location:** `cli/phase/`, `discovery/DiscoveryPhase`

Discovery workflow phases with explicit transitions:
```java
public interface PhaseHandler {
    DiscoveryPhase getPhase();
    PhaseResult execute(PhaseContext context);
}
```

The `DiscoveryPhase` enum defines valid transitions via `next()` and `previous()`.

### Builder Pattern
**Location:** `domain/Topic`, `domain/DomainContext`, `domain/ScopeConfiguration`, `discovery/TopicPromptBuilder`

Complex domain objects use builders:
```java
Topic topic = Topic.builder("Machine Learning")
    .withDescription("Introduction to ML")
    .withComplexity(ComplexityLevel.INTERMEDIATE)
    .build();
```

**TopicPromptBuilder** - Composable builder for LLM prompts:
```java
String prompt = TopicPromptBuilder.create()
    .addDomainContext(domainName)
    .addSeedTopic(seedTopic)
    .addSearchContext(topicInfo, relatedTopics)
    .addExistingTopics(existingTopics)
    .addScopeGuidance(scope)
    .addSearchGroundedTask(suggestionsRange)
    .build();
```

### Registry Pattern
**Location:** `search/SearchProviderRegistry`, `cli/phase/PhaseHandlerRegistry`

Central lookup for typed implementations.

## Package Structure

```
autonomous/           # Autonomous "I Feel Lucky" mode
  AutonomousConfig        # Configuration record with builder
  AutonomousCurator       # AI-powered ACCEPT/REJECT/DEFER decisions
  AutonomousDiscoverySession  # Main orchestrator
  AutonomousScopeInferrer # Infers scope from user description
  StoppingCriteria        # Convergence detection
cli/
  curation/           # Command pattern - user actions
    topic/            # Topic curation commands
    relationship/     # Relationship curation commands
  input/              # Console input helpers
  phase/              # State pattern - phase handlers
config/               # Spring configuration
discovery/            # Core discovery logic (TopicExpander, GapAnalyzer, etc.)
domain/               # Domain model (Topic, TopicUniverse, etc.)
search/               # Search providers (Wikipedia)
util/                 # JSON parsing utilities
```

## Code Conventions

### Java Records for Value Objects
```java
public record TopicSuggestion(
    String name,
    String description,
    String rationale,
    ComplexityLevel complexity
) { }
```

### Enums with Behavior
```java
public enum DiscoveryPhase {
    SEED_INPUT, SCOPE_SETUP, ...;

    public DiscoveryPhase next() { }
    public boolean isSkippable() { }
}
```

### CurationAction Parsing
```java
public enum CurationAction {
    ACCEPT, REJECT, MODIFY, DEFER, SKIP_REST;

    public static CurationAction parse(String input) {
        // Handles shortcuts: "a", "accept", "1", etc.
    }
}
```

## Key APIs

### JsonParsingUtils (util/)
Use for parsing LLM responses that contain JSON:
```java
// Extract and parse JSON from LLM response (handles markdown code blocks)
JsonNode root = JsonParsingUtils.parseJson(response);

// Get field with default fallback
String value = JsonParsingUtils.getStringOrDefault(root, "field", "default");

// Parse string array from field
List<String> items = JsonParsingUtils.parseStringArray(root, "arrayField");
```

### DiscoverySession (discovery/)
Central state manager - use mutation methods:
```java
session.addSeedTopic(name, description);
session.acceptTopicSuggestion(suggestion);
session.confirmRelationship(suggestion);
session.buildUniverse();  // Returns TopicUniverse snapshot
session.getScope();       // Returns ScopeConfiguration
session.getAcceptedTopicCount();
```

### TopicSuggestion Scoring (discovery/)
For autonomous decisions, use combined scoring:
```java
double score = suggestion.getCombinedScore();  // Weighted relevance + search confidence
boolean highQuality = suggestion.meetsAutonomousThreshold(0.75);
boolean lowQuality = suggestion.shouldAutoReject();
```

### CostProfile Bounds (discovery/)
Each cost profile has autonomous bounds:
```java
profile.autonomousMinTopics()  // Stop expanding below this
profile.autonomousMaxTopics()  // Hard cap
profile.convergenceThreshold() // Quality threshold (0.3-0.5)
```

### ConsoleInputHelper (cli/input/)
Utilities for console input handling:
```java
// Parse comma-separated input into trimmed, non-empty list
List<String> items = ConsoleInputHelper.parseCommaSeparated("item1, item2, item3");

// Prompt for user input with validation
InputResult result = input.prompt("Enter value", validator);
```

## Testing

**Run all tests:**
```bash
mvn test
```

**Test organization:** Nested classes by behavior
```java
class TopicSuggestionTest {
    @Nested class BuilderTests { }
    @Nested class Validation { }
}
```

## Integration with aipublisher

Both projects share the `TopicUniverse` JSON format:
- **Storage:** `~/.aipublisher/universes/`
- **Format:** `{id}.universe.json`

Workflow:
1. `aidiscovery --discover` → creates universe
2. `aipublisher --universe {id}` → generates articles

## Build Commands

```bash
mvn test              # Run tests
mvn package           # Build JAR
java -jar target/aidiscovery.jar --help
java -jar target/aidiscovery.jar --discover
java -jar target/aidiscovery.jar --list

# Autonomous mode
java -jar target/aidiscovery.jar --ifeellucky "Domain Name" \
    -d "Description for the AI" \
    -c balanced \
    --llm.provider=ollama \
    --ollama.base-url=http://inference.jakefear.com:11434 \
    -v
```

## Key Files

| File | Lines | Purpose |
|------|-------|---------|
| `DiscoveryInteractiveSession.java` | 1,087 | Main interactive session orchestrator |
| `AutonomousDiscoverySession.java` | 413 | Autonomous mode orchestrator |
| `TopicExpander.java` | 470 | LLM-powered topic suggestions |
| `TopicPromptBuilder.java` | 265 | Composable LLM prompt construction |
| `TopicUniverse.java` | 479 | Domain aggregate root |
| `AiDiscoveryCommand.java` | 500+ | CLI entry point with autonomous options |
| `AutonomousCurator.java` | 315 | AI-powered curation decisions |

## Development Guidelines

1. **Add commands via factory** - New curation actions use `SimpleCurationCommand` via `*CurationCommandFactory`; only create custom commands for complex interactive logic
2. **Add phases via registry** - New phases implement `PhaseHandler` and register in `PhaseHandlerRegistry`
3. **Keep DiscoveryInteractiveSession focused** - Use `executePhase()` helper for consistent phase execution
4. **Build prompts with TopicPromptBuilder** - Use composable builder instead of string concatenation
5. **Test curation flows** - Each command needs tests for execute behavior

## Anti-Patterns to Avoid

- **Bypassing factories** - Always use factories to get commands
- **Direct phase transitions** - Use `DiscoveryPhase.next()` not hardcoded values
- **Modifying TopicUniverse directly** - Use its mutation methods that maintain invariants
- **Creating single-use command classes** - Use `SimpleCurationCommand` with method reference
- **String concatenation for prompts** - Use `TopicPromptBuilder` for maintainability

---

*Sibling project: [aipublisher](https://github.com/jakefearsd/aipublisher)*
