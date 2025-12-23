# AI Discovery Architecture

This document describes the internal architecture and design of AI Discovery. For user documentation, see [README.md](README.md).

## Table of Contents

- [System Overview](#system-overview)
- [Package Structure](#package-structure)
- [Core Components](#core-components)
- [Discovery Modes](#discovery-modes)
- [Search Grounding](#search-grounding)
- [AI Integration](#ai-integration)
- [Data Flow](#data-flow)
- [Configuration](#configuration)

---

## System Overview

AI Discovery is a Spring Boot CLI application that builds **Topic Universes** - structured plans for wiki content. It uses LLMs (Anthropic Claude or local Ollama models) combined with knowledge base grounding (Wikidata/Wikipedia) to discover, validate, and organize topics.

```
┌─────────────────────────────────────────────────────────────────────┐
│                           User Input                                 │
│                   (Domain, Description, Seeds)                       │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        CLI Layer (Picocli)                           │
│  ┌─────────────────────┐    ┌────────────────────────────────────┐  │
│  │  Interactive Mode   │    │      Autonomous Mode               │  │
│  │  (8-phase workflow) │    │  ("I Feel Lucky" - 6 phases)       │  │
│  └─────────────────────┘    └────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Discovery Services                              │
│  ┌─────────────────┐ ┌──────────────────┐ ┌─────────────────────┐   │
│  │ TopicExpander   │ │RelationshipSuggest│ │   GapAnalyzer      │   │
│  │ (AI + Search)   │ │       er          │ │                    │   │
│  └─────────────────┘ └──────────────────┘ └─────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
┌──────────────────────────────┐  ┌───────────────────────────────────┐
│       LLM Integration        │  │      Search Grounding             │
│  ┌────────────────────────┐  │  │  ┌─────────────┐ ┌─────────────┐  │
│  │  Anthropic (Claude)    │  │  │  │  Wikidata   │ │  Wikipedia  │  │
│  │  Ollama (qwen3:14b)    │  │  │  │  (default)  │ │  (fallback) │  │
│  └────────────────────────┘  │  │  └─────────────┘ └─────────────┘  │
└──────────────────────────────┘  └───────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       Topic Universe (JSON)                          │
│            ~/.aipublisher/universes/<id>.universe.json               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Package Structure

```
com.jakefear.aidiscovery
├── AiDiscoveryApplication.java      # Spring Boot entry point
│
├── cli/                             # Command-line interface
│   ├── AiDiscoveryCommand.java      # Main Picocli command
│   ├── DiscoveryInteractiveSession.java  # 8-phase interactive mode
│   ├── SessionLogger.java           # Session logging
│   ├── curation/                    # Topic/relationship curation commands
│   │   ├── SimpleCurationCommand.java  # Generic command with functional delegation
│   │   ├── topic/                   # Modify, SkipRest (complex commands)
│   │   └── relationship/            # ChangeType (complex command)
│   ├── input/                       # Console input helpers
│   └── phase/                       # Phase handlers (Seed, Scope, Review)
│
├── autonomous/                      # "I Feel Lucky" mode
│   ├── AutonomousConfig.java        # Configuration record
│   ├── AutonomousContext.java       # AI decision context
│   ├── AutonomousCurator.java       # AI-powered accept/reject
│   ├── AutonomousDiscoverySession.java  # 6-phase orchestrator
│   ├── AutonomousProgressReporter.java  # Console output
│   ├── AutonomousScopeInferrer.java # Scope inference from description
│   └── StoppingCriteria.java        # When to stop expansion
│
├── config/                          # Spring configuration
│   ├── LlmConfig.java               # Anthropic/Ollama setup
│   └── JacksonConfig.java           # JSON serialization
│
├── discovery/                       # Core discovery services
│   ├── TopicExpander.java           # Topic suggestion generation
│   ├── TopicPromptBuilder.java      # Composable prompt construction
│   ├── TopicSuggestion.java         # Suggestion with scores
│   ├── RelationshipSuggester.java   # Relationship analysis
│   ├── RelationshipSuggestion.java  # Relationship with confidence
│   ├── GapAnalyzer.java             # Coverage gap detection
│   ├── CostProfile.java             # Resource usage presets
│   ├── DiscoverySession.java        # Session state management
│   ├── DiscoveryPhase.java          # Phase enumeration
│   ├── DomainContext.java           # Domain themes/glossary
│   └── RelationshipDepth.java       # Relationship analysis depth
│
├── domain/                          # Domain models
│   ├── Topic.java                   # Topic entity
│   ├── TopicUniverse.java           # Complete universe
│   ├── TopicRelationship.java       # Relationship entity
│   ├── ScopeConfiguration.java      # Scope boundaries
│   ├── TopicUniverseRepository.java # JSON persistence
│   ├── TopicStatus.java             # PROPOSED, ACCEPTED, etc.
│   ├── Priority.java                # MUST_HAVE, SHOULD_HAVE, etc.
│   ├── ContentType.java             # CONCEPT, TUTORIAL, etc.
│   ├── ComplexityLevel.java         # BEGINNER to EXPERT
│   └── RelationshipType.java        # PREREQUISITE_OF, PART_OF, etc.
│
├── search/                          # Search grounding providers
│   ├── SearchProvider.java          # Provider interface
│   ├── SearchProviderRegistry.java  # Provider management
│   ├── SearchResult.java            # Search result record
│   ├── SourceReliability.java       # AUTHORITATIVE, REPUTABLE, etc.
│   ├── WikidataSearchService.java   # Wikidata API (default)
│   ├── WikipediaSearchService.java  # Wikipedia API (fallback)
│   └── WebSearchService.java        # Generic web search
│
└── util/
    └── JsonParsingUtils.java        # JSON parsing helpers
```

---

## Core Components

### TopicExpander

The heart of discovery. Generates topic suggestions using:
1. **LLM reasoning** - Understands domain context, suggests related topics
2. **Search grounding** - Validates suggestions against Wikidata/Wikipedia
3. **Score calculation** - Combines relevance (LLM) with confidence (search)

```java
public List<TopicSuggestion> expandTopicWithSearch(
    String topic,           // Topic to expand from
    String domainName,      // Domain context
    Set<String> existing,   // Already accepted topics
    ScopeConfiguration scope,  // Scope boundaries
    CostProfile costProfile    // Resource limits
)
```

Each `TopicSuggestion` includes:
- `relevanceScore` (0.0-1.0) - LLM's assessment of topic relevance
- `searchConfidence` (0.0-1.0) - Wikidata validation confidence
- `getCombinedScore()` - Weighted combination for curation decisions

### TopicPromptBuilder

Composable builder for constructing LLM prompts. Eliminates duplication across different prompt types:

```java
String prompt = TopicPromptBuilder.create()
    .addDomainContext(domainName)
    .addSeedTopic(seedTopic)
    .addSearchContext(topicInfo, relatedTopics)  // Optional
    .addExistingTopics(existingTopics)
    .addScopeGuidance(scope)
    .addSearchGroundedTask(suggestionsRange)     // Or addStandardTask()
    .build();
```

Provides consistent prompt sections:
- **Domain context** - Domain name and optional description
- **Seed topic** - Topic being expanded
- **Search context** - Grounding from Wikidata/Wikipedia
- **Existing topics** - Duplicate prevention
- **Scope guidance** - Assumed knowledge, exclusions, focus areas
- **Task sections** - Standard, search-grounded, or initial topics

### SimpleCurationCommand

Generic command for simple curation actions that use functional delegation:

```java
// Factory creates commands with session method references
new SimpleCurationCommand<>(
    CurationAction.ACCEPT,
    "Accepted",
    DiscoverySession::acceptTopicSuggestion
);
```

Consolidates 5 nearly-identical command classes into 1 parameterized class. Complex commands like `ModifyTopicCommand` and `ChangeTypeRelationshipCommand` remain as separate classes since they require user interaction.

### AutonomousCurator

AI-powered decision maker for autonomous mode. Uses rules + AI:

```
FOR each TopicSuggestion:
  combinedScore = (relevance * 0.6) + (searchConfidence * 0.4)

  IF combinedScore >= 0.75 AND searchConfidence >= 0.3:
    AUTO_ACCEPT (high confidence)
  ELSE IF combinedScore < 0.4 OR searchConfidence < 0.2:
    AUTO_REJECT (likely hallucinated)
  ELSE:
    ASK_AI for reasoning → ACCEPT/REJECT/DEFER
```

### DiscoverySession

State management for the discovery process:
- Tracks current phase
- Maintains topic/relationship collections
- Handles accept/reject/defer actions
- Builds final TopicUniverse

---

## Discovery Modes

### Interactive Mode (8 Phases)

Human-in-the-loop discovery with full control:

| Phase | Description | User Actions |
|-------|-------------|--------------|
| 1. Seed Input | Domain name, initial topics | Provide seeds, select landing page |
| 2. Scope Setup | Define boundaries | Set assumed knowledge, exclusions, focus |
| 3. Topic Expansion | AI suggests topics | Accept, Reject, Defer, Modify each |
| 4. Relationship Mapping | Define connections | Confirm, Reject, Change type |
| 5. Gap Analysis | Identify missing topics | Add suggested topics |
| 6. Depth Calibration | Adjust word counts | Modify estimates per topic |
| 7. Prioritization | Set generation order | Assign MUST/SHOULD/NICE_TO_HAVE |
| 8. Review | Final approval | Confirm and save |

**Phase Execution Pattern:**

The `run()` method uses a helper pattern for consistent phase execution:

```java
// Each phase is executed with logging and cancellation handling
if (!executePhase(DiscoveryPhase.SEED_INPUT, this::runSeedInputPhase)) return null;
if (!executePhase(DiscoveryPhase.SCOPE_SETUP, this::runScopeSetupPhase)) return null;
// ... remaining phases
```

The `executePhase()` helper handles:
- Phase logging (`sessionLog.phase()`)
- Cancellation detection and reporting
- State logging after completion

### Autonomous Mode (6 Phases)

Fully automated "I Feel Lucky" mode:

| Phase | Description | AI Behavior |
|-------|-------------|-------------|
| 1. Scope Inference | Infer boundaries from description | Determines audience, exclusions, seeds |
| 2. Topic Expansion | Iterative expansion with auto-curation | Rules + AI accept/reject |
| 3. Relationship Mapping | Auto-confirm high-confidence relationships | Threshold-based approval |
| 4. Gap Analysis | Auto-fill critical gaps | Adds topics for critical gaps only |
| 5. Prioritization | Rules-based priority assignment | Prerequisites → MUST_HAVE |
| 6. Finalization | Build and save universe | No user interaction |

---

## Search Grounding

Search grounding prevents AI hallucination by validating suggestions against knowledge bases.

### Wikidata (Default)

- Uses `wbsearchentities` API for flexible matching
- Searches labels AND aliases (more forgiving than Wikipedia)
- Validates composite topics by checking component words
- No API key required

**Validation Logic:**
```
exact match      → 1.0
contains match   → 0.85
word overlap     → 0.5-0.85 (proportional)
partial words    → 0.35-0.6
no match         → 0.0
```

### Wikipedia (Fallback)

- Uses MediaWiki API with exact title matching
- More strict validation
- Returns 404 for composite topics like "Voice Assistant Development"

### Provider Selection

```java
@Value("${search.default-provider:wikidata}")
private String defaultProviderName;
```

---

## AI Integration

### LLM Configuration

Supports two providers:

**Anthropic (Cloud)**
```properties
llm.provider=anthropic
anthropic.api.key=${ANTHROPIC_API_KEY}
anthropic.model=claude-sonnet-4-20250514
```

**Ollama (Local)**
```properties
llm.provider=ollama
ollama.base-url=http://localhost:11434
ollama.model=qwen3:14b
```

### Model Beans

Two chat model beans with different temperature settings:

| Bean | Temperature | Purpose |
|------|-------------|---------|
| `chatLanguageModel` | 0.5 | General use |
| `researchChatModel` | 0.3 | Topic expansion, analysis (factual) |

### Prompt Engineering

All AI prompts follow a consistent structure:
1. System prompt with role and constraints
2. Context (domain, scope, existing topics)
3. Specific task with output format
4. JSON response requirement

Example from TopicExpander:
```
You are a domain expert helping to plan comprehensive wiki content...

## Context
- Domain: {domainName}
- Current topic: {topic}
- Existing topics: {existingList}
- Scope boundaries: {scope}

## Task
Suggest {n} related topics...

## Response Format
```json
{ "suggestions": [...] }
```
```

---

## Data Flow

### Topic Expansion Flow

```
User/Auto Input
      │
      ▼
┌─────────────┐    Prompt with     ┌─────────────┐
│   Topic     │────context────────►│    LLM      │
│  Expander   │                    │             │
└─────────────┘                    └──────┬──────┘
      │                                   │
      │                           JSON suggestions
      │                                   │
      ▼                                   ▼
┌─────────────┐    Validate        ┌─────────────┐
│   Search    │◄───each topic──────│   Parse &   │
│  Provider   │                    │   Score     │
└─────────────┘                    └─────────────┘
      │                                   │
  confidence                              │
   scores                                 │
      │                                   ▼
      └──────────────────────────►┌─────────────┐
                                  │ Combined    │
                                  │ Suggestions │
                                  └─────────────┘
                                        │
                                        ▼
                               ┌─────────────────┐
                               │ Curation        │
                               │ (Human or AI)   │
                               └─────────────────┘
                                        │
                                        ▼
                               ┌─────────────────┐
                               │ Topic Universe  │
                               └─────────────────┘
```

### Autonomous Mode Flow

```
./ifeellucky.sh "Domain" "Description"
                │
                ▼
┌───────────────────────────────┐
│ 1. Scope Inference            │
│    - Parse description        │
│    - Infer audience, seeds    │
│    - Set boundaries           │
└───────────────────────────────┘
                │
                ▼
┌───────────────────────────────┐
│ 2. Topic Expansion (N rounds) │
│    FOR each round:            │
│      - Expand unexpanded      │
│      - Validate via Wikidata  │◄─── Search Grounding
│      - Auto-curate (rules+AI) │
│      - Check stopping criteria│
└───────────────────────────────┘
                │
                ▼
┌───────────────────────────────┐
│ 3. Relationship Mapping       │
│    - Analyze all pairs        │
│    - Auto-confirm conf >= 0.8 │
└───────────────────────────────┘
                │
                ▼
┌───────────────────────────────┐
│ 4. Gap Analysis               │
│    - Identify missing content │
│    - Auto-fill critical gaps  │
└───────────────────────────────┘
                │
                ▼
┌───────────────────────────────┐
│ 5. Prioritization             │
│    - Landing page → MUST_HAVE │
│    - Prerequisites → MUST_HAVE│
│    - Seeds → MUST_HAVE        │
│    - Others → SHOULD_HAVE     │
└───────────────────────────────┘
                │
                ▼
┌───────────────────────────────┐
│ 6. Finalization               │
│    - Build TopicUniverse      │
│    - Save to JSON             │
└───────────────────────────────┘
```

---

## Configuration

### Cost Profiles

| Profile | Rounds | Topics/Round | Suggestions | Max Complexity |
|---------|--------|--------------|-------------|----------------|
| MINIMAL | 1 | 2 | 4 | Intermediate |
| BALANCED | 3 | 3 | 7 | Advanced |
| COMPREHENSIVE | 5 | 5 | 12 | Expert |

### Stopping Criteria (Autonomous)

Discovery stops when ANY condition is met:
- `topicCount >= maxTopics` (from cost profile)
- `expansionRounds >= maxRounds`
- 3 consecutive rounds with < 2 high-quality suggestions
- All critical gaps addressed

### Application Properties

```properties
# LLM Provider
llm.provider=anthropic|ollama
llm.temperature.research=0.3

# Anthropic
anthropic.api.key=${ANTHROPIC_API_KEY}
anthropic.model=claude-sonnet-4-20250514
anthropic.max-tokens=4096

# Ollama
ollama.base-url=http://localhost:11434
ollama.model=qwen3:14b
ollama.timeout=PT5M
ollama.num-predict=4096

# Search
search.default-provider=wikidata
search.max-results=5
search.wikidata.enabled=true
search.wikipedia.enabled=true

# Storage
discovery.storage-directory=${user.home}/.aipublisher/universes
```

---

## Extension Points

### Adding a Search Provider

1. Implement `SearchProvider` interface
2. Add `@Service` annotation
3. Auto-registered via `SearchProviderRegistry`

```java
@Service
public class MySearchService implements SearchProvider {
    @Override
    public String getProviderName() { return "mysearch"; }

    @Override
    public List<SearchResult> search(String query) { ... }

    @Override
    public double validateTopic(String topic) { ... }
}
```

### Adding a New Phase

1. Create phase handler implementing `PhaseHandler`
2. Register in `PhaseHandlerRegistry`
3. Add to `DiscoveryPhase` enum

### Custom Cost Profile

Create new `CostProfile` instance with desired parameters:
```java
public static final CostProfile CUSTOM = new CostProfile(
    "Custom",
    "My custom profile",
    4,      // maxExpansionRounds
    4,      // topicsPerRound
    10,     // suggestionsPerTopic
    ComplexityLevel.ADVANCED,
    1.2,    // wordCountMultiplier
    false,  // skipGapAnalysis
    RelationshipDepth.IMPORTANT,
    0.75    // autoAcceptThreshold
);
```
