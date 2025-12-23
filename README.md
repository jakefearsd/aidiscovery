# AI Discovery

An interactive CLI tool for building comprehensive topic universes through AI-assisted domain discovery. Work with AI to map out your entire knowledge domain, curate topics, define relationships, and identify gaps before generating any content.

## Overview

AI Discovery guides you through an 8-phase workflow to build a **Topic Universe** - a structured plan for your entire wiki. Instead of generating articles one at a time, you work with AI to strategically plan your content, curate topics, and define how they connect.

### Why Use AI Discovery?

- **Strategic Planning** - Map your entire knowledge domain before writing
- **Human-in-the-Loop** - AI suggests, you curate and refine
- **Relationship Mapping** - Define how topics connect (prerequisites, related concepts)
- **Gap Analysis** - AI identifies missing topics and coverage gaps
- **Generation Ordering** - Topological sort ensures prerequisites are written first
- **Scope Control** - Define what's in/out of scope, assumed knowledge

### Workflow Integration

```
aidiscovery              -->  Creates topic universe
aipublisher --universe   -->  Generates articles from universe
```

## Requirements

- Java 21 or later
- Maven 3.8+
- One of the following LLM providers:
  - **Anthropic API key** ([Get one here](https://console.anthropic.com/settings/keys)) - Cloud-based, paid
  - **Ollama server** ([Install Ollama](https://ollama.ai)) - Local inference, free

## Quick Start

### 1. Build the Project

```bash
git clone https://github.com/jakefearsd/aidiscovery.git
cd aidiscovery
mvn clean package -DskipTests
```

### 2. Configure Your LLM Provider

**Option A: Anthropic (Claude API)**
```bash
export ANTHROPIC_API_KEY='your-api-key-here'
```

**Option B: Ollama (Local Inference - Free)**
```bash
ollama pull qwen3:14b
java -jar target/aidiscovery.jar --llm.provider=ollama
```

### 3. Start a Discovery Session

```bash
# Interactive discovery (prompts for cost profile)
java -jar target/aidiscovery.jar

# Quick prototype (~5 minutes, ~$1 API cost)
java -jar target/aidiscovery.jar -c minimal

# Standard project (~15 minutes, ~$5 API cost)
java -jar target/aidiscovery.jar --cost-profile balanced

# Enterprise documentation (~30 minutes, ~$15 API cost)
java -jar target/aidiscovery.jar -c comprehensive
```

## Cost Profiles

Cost profiles control how thoroughly the AI explores your domain. Higher profiles generate more topics but consume more API credits.

| Profile | Topics | Rounds | Est. Discovery Cost | Est. Content Cost | Best For |
|---------|--------|--------|---------------------|-------------------|----------|
| **MINIMAL** | 2-4 | 1 | $0.50-2 | $5-15 | Prototyping, testing ideas, small personal wikis |
| **BALANCED** | 9-31 | 3 | $2-5 | $30-75 | Most wikis, documentation projects, team knowledge bases |
| **COMPREHENSIVE** | 25-150 | 5 | $5-15 | $100-250 | Enterprise documentation, complete technical references |

**Profile Settings Breakdown:**

| Setting | MINIMAL | BALANCED | COMPREHENSIVE |
|---------|---------|----------|---------------|
| Max expansion rounds | 1 | 3 | 5 |
| Topics per round | 2 | 3 | 5 |
| Suggestions per topic | 2-6 | 5-9 | 10-14 |
| Max complexity | Intermediate | Advanced | Expert |
| Word count multiplier | 0.6x | 1.0x | 1.5x |
| Gap analysis | Skipped | Enabled | Enabled |
| Relationship depth | Core only | Important | All |

## The 8 Discovery Phases

### Phase 1: Seed Input

Provide your domain name and initial seed topics - the core subjects you definitely want to cover.

```
╔═══════════════════════════════════════════════════════════════════╗
║              AI DISCOVERY - DOMAIN DISCOVERY MODE                 ║
╚═══════════════════════════════════════════════════════════════════╝

What domain or subject area is this wiki about?

Examples:
  - Apache Kafka
  - Cloud Native Development
  - Machine Learning Operations

Domain name: Apache Kafka

Enter your initial seed topics (one per line, empty line to finish):
Seed topic 1: Kafka Producers
  Brief description: How to send messages to Kafka
Seed topic 2: Kafka Consumers
  Brief description: How to read messages from Kafka
Seed topic 3:

Which topic should be the main landing page?
  1. Kafka Producers
  2. Kafka Consumers
Selection [1]: 1

✓ Created domain 'Apache Kafka' with 2 seed topics
```

### Phase 2: Scope Setup (Optional)

Define boundaries to help AI generate more relevant suggestions.

```
Configure scope? [Y/n/skip]: y

What knowledge should readers already have? (comma-separated)
Examples: Java programming, basic SQL, command line familiarity
Assumed knowledge: Java programming, basic distributed systems

What topics should be explicitly excluded? (comma-separated)
Out of scope: Kafka Streams (separate wiki), Kafka Connect

Any specific areas to prioritize? (comma-separated)
Focus areas: Production deployment, Performance tuning

Target audience description: Backend developers new to event streaming
```

### Phase 3: Topic Expansion

AI analyzes your seed topics and suggests related topics. You curate each suggestion.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Expanding from: Kafka Producers
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌──────────────────────────────────────────────────────────┐
│ 1/7: Producer Configuration                              │
├──────────────────────────────────────────────────────────┤
│ Essential settings for Kafka producers including acks,   │
│ retries, batch size, and compression options.            │
│ Category: component           Relevance: ████████░░ 80%  │
│ Type: Reference               Complexity: Intermediate   │
│ Why: Critical for production deployments                 │
└──────────────────────────────────────────────────────────┘

  [A]ccept  [R]eject  [D]efer  [M]odify  [S]kip rest  [Q]uit
  Decision: a
  ✓ Accepted

┌──────────────────────────────────────────────────────────┐
│ 2/7: Message Serialization                               │
├──────────────────────────────────────────────────────────┤
│ How to serialize messages using Avro, JSON, or Protobuf  │
│ Category: prerequisite        Relevance: ███████░░░ 70%  │
│ Type: Concept                 Complexity: Intermediate   │
│ Why: Understanding serialization is essential            │
└──────────────────────────────────────────────────────────┘

  [A]ccept  [R]eject  [D]efer  [M]odify  [S]kip rest  [Q]uit
  Decision: m

  Current name: Message Serialization
  New name [keep current]: Kafka Serialization Formats
  Current description: How to serialize messages using Avro...
  New description [keep current]:
  ✓ Modified and accepted
```

**Curation Options:**
- **Accept** - Add topic to universe as-is
- **Reject** - Skip this topic entirely
- **Defer** - Save to backlog for later consideration
- **Modify** - Change name/description before accepting
- **Skip rest** - Auto-accept high-relevance, defer low-relevance

### Phase 4: Relationship Mapping

AI suggests how topics relate to each other. You confirm or modify relationships.

```
Analyzing relationships between your 12 topics...

Found 15 potential relationships. Review the important ones:

● Kafka Basics ──[PREREQUISITE_OF]──> Kafka Producers
  └─ Understanding core concepts is essential before producing
  [C]onfirm  [R]eject  [T]ype change: c
  ✓ Confirmed

◐ Producer Configuration ──[PART_OF]──> Kafka Producers
  └─ Configuration is a component of producer setup
  [C]onfirm  [R]eject  [T]ype change: t

  Select relationship type:
    1. Prerequisite Of
    2. Part Of ← current
    3. Example Of
    4. Related To
    5. Contrasts With
    6. Implements
    7. Supersedes
    8. Pairs With
  Selection: 4
  ✓ Confirmed as RELATED_TO
```

**Relationship Types:**

| Type | Meaning | Example |
|------|---------|---------|
| `PREREQUISITE_OF` | Must understand A before B | Java -> Spring Boot |
| `PART_OF` | A is a component of B | Partitions -> Topics |
| `EXAMPLE_OF` | A is an instance of B | Avro -> Serialization |
| `RELATED_TO` | Related but neither prerequisite | Producers <-> Consumers |
| `CONTRASTS_WITH` | Alternatives or opposites | Kafka vs RabbitMQ |
| `IMPLEMENTS` | A implements concept B | KafkaProducer -> Producer API |
| `SUPERSEDES` | A replaces B | New API -> Legacy API |
| `PAIRS_WITH` | Commonly used together | Producers + Schema Registry |

### Phase 5: Gap Analysis

AI analyzes your topic coverage and identifies potential gaps.

```
Analyzing topic coverage for gaps...

Coverage Assessment:
  Coverage:      ████████░░ 80%
  Balance:       ███████░░░ 70%
  Connectedness: █████████░ 90%

Summary: Good coverage of producer topics but consumer side needs more depth.

Found 3 gaps to review:

🔴 [MISSING_PREREQUISITE] Consumer Group Coordination
   Resolution: Add topic explaining how consumer groups coordinate

   Add suggested topic 'Consumer Group Coordination'? [Y/n]: y
   ✓ Topic added

🟡 [COVERAGE_GAP] Error handling patterns not covered
   Resolution: Add troubleshooting guide for common producer errors

   Add suggested topic 'Producer Error Handling'? [Y/n]: y
   ✓ Topic added

🟢 [DEPTH_IMBALANCE] Security topics are shallow
   Resolution: Consider expanding authentication/authorization coverage

   Add suggested topic 'Kafka Security'? [Y/n]: n
   → Skipped
```

**Gap Severity Levels:**
- 🔴 **Critical** - Missing essential prerequisite or core concept
- 🟡 **Moderate** - Notable gap that should be addressed
- 🟢 **Minor** - Nice to have, can be addressed later

### Phase 6: Depth Calibration (Optional)

Adjust word counts and complexity levels for each topic.

```
Would you like to adjust topic depths?
Calibrate depths? [y/N/skip]: y

Current topics and suggested word counts:

   1. Kafka Producers                Intermediate (1000 words)
   2. Producer Configuration         Intermediate (1000 words)
   3. Kafka Serialization Formats    Intermediate (1000 words)
   4. Consumer Group Coordination    Advanced (1500 words)
   ...

Enter topic number to adjust, or press Enter to finish:
Topic #: 4
  Current: Advanced (1500 words)
  New word count: 2000
  ✓ Updated

Topic #:
```

### Phase 7: Prioritization

Assign generation priorities to control which topics are written first.

```
Review topic priorities:

  Priority levels:
    1. MUST_HAVE   - Essential, generate first
    2. SHOULD_HAVE - Important, generate second
    3. NICE_TO_HAVE - Optional, generate if time permits
    4. BACKLOG     - Future consideration

  MUST_HAVE (3 topics):
    - Kafka Producers
    - Kafka Consumers
    - Kafka Basics

  SHOULD_HAVE (8 topics):
    - Producer Configuration
    - Consumer Configuration
    - Kafka Serialization Formats
    ...

Adjust priorities? [y/N]: y

Enter topic name and new priority (e.g., 'Kafka Security 1'):
> Kafka Security 1
  ✓ Updated
>
```

### Phase 8: Review

Final review before saving the topic universe.

```
╔═══════════════════════════════════════════════════════════════════╗
║                        DISCOVERY SUMMARY                          ║
╚═══════════════════════════════════════════════════════════════════╝

  Domain:        Apache Kafka
  Topics:        14 accepted
  Relationships: 23 mapped
  Backlog:       3 items

  Suggested generation order:
     1. Kafka Basics [MUST_HAVE]
     2. Kafka Producers [MUST_HAVE]
     3. Kafka Consumers [MUST_HAVE]
     4. Producer Configuration [SHOULD_HAVE]
     5. Consumer Configuration [SHOULD_HAVE]
     6. Kafka Serialization Formats [SHOULD_HAVE]
     7. Consumer Group Coordination [SHOULD_HAVE]
     8. Producer Error Handling [SHOULD_HAVE]
     9. Kafka Security [MUST_HAVE]
    10. Message Retention [SHOULD_HAVE]
    ... and 4 more

───────────────────────────────────────────────────────────────────

Finalize this topic universe? [Y/n]: y

✓ Topic universe finalized!

Session ID: a1b2c3d4

═══════════════════════════════════════════════════════════════════
Topic universe saved!

  ID:       kafka-wiki-2024-01
  Name:     Apache Kafka
  Topics:   14 accepted
  Location: ~/.aipublisher/universes/kafka-wiki-2024-01.universe.json

To generate articles from this universe, use:
  aipublisher --universe kafka-wiki-2024-01
═══════════════════════════════════════════════════════════════════
```

## Topic Universe Data Model

The discovery session creates a `TopicUniverse` - a structured representation of your wiki's content plan:

```
TopicUniverse
├── id: "kafka-wiki-2024-01"
├── name: "Apache Kafka"
├── description: "Comprehensive Kafka documentation for developers"
├── topics: [
│   ├── Topic {
│   │   id: "KafkaProducers"
│   │   name: "Kafka Producers"
│   │   status: ACCEPTED
│   │   priority: MUST_HAVE
│   │   contentType: TUTORIAL
│   │   complexity: INTERMEDIATE
│   │   estimatedWords: 1500
│   │   emphasize: ["performance", "error handling"]
│   │   skip: ["legacy APIs"]
│   │   isLandingPage: true
│   │   }
│   └── ...
│   ]
├── relationships: [
│   ├── TopicRelationship {
│   │   source: "KafkaBasics"
│   │   target: "KafkaProducers"
│   │   type: PREREQUISITE_OF
│   │   status: CONFIRMED
│   │   }
│   └── ...
│   ]
├── scope: {
│   assumedKnowledge: ["Java programming"]
│   outOfScope: ["Kafka Streams"]
│   focusAreas: ["Production deployment"]
│   audienceDescription: "Backend developers new to event streaming"
│   }
└── backlog: ["Kafka Connect Integration", ...]
```

## Saved Universe Location

Topic universes are saved to:
```
~/.aipublisher/universes/<universe-id>.universe.json
```

You can:
- View saved universes with any JSON viewer
- Edit them manually if needed
- Share them with team members
- Version control them alongside your wiki

## Scope Configuration Reference

The `scope` section of a universe file controls how content is written across all articles.

```json
"scope": {
  "audienceDescription": "Backend developers new to event streaming who have 1-2 years of programming experience",
  "domainDescription": "Apache Kafka ecosystem for building real-time data pipelines and streaming applications",
  "intent": "Educational tutorials that build understanding progressively. Use clear explanations, practical examples, and a friendly but professional tone. Focus on why concepts matter, not just how they work.",
  "assumedKnowledge": ["Java programming basics", "Command line familiarity", "Basic networking concepts"],
  "outOfScope": ["Kafka Streams (covered in separate wiki)", "Kafka Connect connectors", "Cloud-specific deployments"],
  "focusAreas": ["Production deployment", "Performance tuning", "Error handling patterns"],
  "preferredLanguage": "Java"
}
```

### Scope Fields

| Field | Purpose | How It Influences Content |
|-------|---------|---------------------------|
| `audienceDescription` | **Who** the readers are | Adjusts vocabulary, depth of explanation, and assumed context |
| `domainDescription` | **What** domain/context | Provides context for the AI to understand the subject area |
| `intent` | **Why/How** to write | Controls tone, style, and purpose |
| `assumedKnowledge` | What readers already know | Listed concepts won't be explained in detail |
| `outOfScope` | What to exclude | Topics listed here are explicitly avoided |
| `focusAreas` | What to emphasize | These aspects receive extra attention and depth |
| `preferredLanguage` | Programming language | Code examples use this language |

### Intent Examples

**Educational/Tutorial Style:**
```json
"intent": "Educational tutorials for complete beginners. Build concepts step-by-step with simple language, relatable analogies, and hands-on examples. Explain the 'why' before the 'how'. Use a friendly, encouraging tone."
```

**Technical Reference Style:**
```json
"intent": "Comprehensive technical reference for experienced practitioners. Be precise and thorough. Include edge cases, performance considerations, and implementation details. Maintain formal, authoritative tone."
```

**Quick Reference/Cheatsheet Style:**
```json
"intent": "Quick reference guides for daily use. Be concise and scannable. Use bullet points, tables, and code snippets. Minimize prose - readers want answers fast."
```

## Manually Editing Universe Files

Universe files are standard JSON and can be edited with any text editor.

### Topic Fields Reference

| Field | Type | Description | Effect on Generation |
|-------|------|-------------|---------------------|
| `id` | string | Unique identifier (CamelCase) | Used for file naming and relationships |
| `name` | string | Display name | Article title |
| `description` | string | What the topic covers | Guides research and content focus |
| `status` | enum | PROPOSED, ACCEPTED, REJECTED, GENERATED, DEFERRED | Only ACCEPTED topics are generated |
| `contentType` | enum | CONCEPT, TUTORIAL, REFERENCE, HOW_TO, TROUBLESHOOTING, COMPARISON | Controls article structure and style |
| `complexity` | enum | BEGINNER, INTERMEDIATE, ADVANCED, EXPERT | Adjusts depth and prerequisite assumptions |
| `priority` | enum | MUST_HAVE, SHOULD_HAVE, NICE_TO_HAVE, BACKLOG | Generation order (MUST_HAVE first) |
| `estimatedWords` | int | Target word count | Article length |
| `emphasize` | array | Aspects to highlight | Extra focus on these areas |
| `skip` | array | Aspects to minimize | These won't be covered in depth |
| `userNotes` | string | Personal notes | Not used in generation, for your reference |
| `isLandingPage` | bool | Main entry point | Gets summary/overview treatment |
| `category` | string | Wiki category | Used in article metadata |

### Content Types

| Type | Best For | Typical Structure |
|------|----------|-------------------|
| `CONCEPT` | Explaining ideas, theories | Definition -> Explanation -> Examples -> See Also |
| `TUTORIAL` | Step-by-step learning | Overview -> Prerequisites -> Steps -> Verification |
| `REFERENCE` | API docs, specifications | Synopsis -> Parameters -> Returns -> Examples |
| `HOW_TO` | Task completion guides | Goal -> Prerequisites -> Steps -> Troubleshooting |
| `TROUBLESHOOTING` | Problem solving | Symptom -> Cause -> Solution -> Prevention |
| `COMPARISON` | Evaluating options | Overview -> Criteria -> Comparison Table -> Recommendations |

### Relationship Types

| Type | Meaning | Generation Effect |
|------|---------|-------------------|
| `PREREQUISITE_OF` | A must be understood before B | A generates before B; B links back to A |
| `PART_OF` | A is a component of B | A may be more detailed; B provides overview |
| `RELATED_TO` | Loosely connected | Cross-linking between articles |
| `EXAMPLE_OF` | A is an instance of B | A inherits context from B |
| `CONTRASTS_WITH` | Alternatives | Comparison content generated |
| `IMPLEMENTS` | A implements pattern B | A references B's concepts |
| `SUPERSEDES` | A replaces B | B marked as legacy if present |
| `PAIRS_WITH` | Commonly used together | Both mention each other |

## Command Line Reference

```
Usage: aidiscovery [-hqvV] [-c=<costProfile>] [-k=<apiKey>] [--key-file=<keyFile>]
                   [--list] [--show=<id>] [--export <id> <path>]

Build comprehensive topic universes for wiki content with AI assistance.

Options:
  -c, --cost-profile=<profile>
                             Cost profile: MINIMAL, BALANCED, or COMPREHENSIVE
                               (prompts if not specified)
      --list                 List all saved topic universes
      --show=<id>            Show details of a saved universe by ID
      --export <id> <path>   Export universe to a file
  -q, --quiet                Suppress non-essential output
  -v, --verbose              Enable verbose output
  -h, --help                 Show this help message and exit.
  -V, --version              Print version information and exit.

LLM Provider Options:
      --llm.provider=<provider>
                             LLM provider: "anthropic" or "ollama" (default: anthropic)
      --ollama.base-url=<url>
                             Ollama server URL (default: http://localhost:11434)
      --ollama.model=<model> Ollama model name (default: qwen3:14b)
      --anthropic.model=<model>
                             Anthropic model name (default: claude-sonnet-4-20250514)

API Key Options:
  -k, --key=<apiKey>         Anthropic API key (overrides environment variable)
      --key-file=<keyFile>   Path to file containing Anthropic API key
      ANTHROPIC_API_KEY      Environment variable (default)

Examples:
  aidiscovery                                     # Interactive discovery
  aidiscovery --cost-profile minimal              # Quick prototype mode
  aidiscovery -c balanced                         # Standard coverage
  aidiscovery -c comprehensive                    # Full enterprise coverage
  aidiscovery --list                              # List all saved universes
  aidiscovery --show my-wiki-id                   # Show universe details
  aidiscovery --export my-wiki-id ./universe.json # Export universe to file

  # Using Ollama (local inference)
  aidiscovery --llm.provider=ollama
  aidiscovery --llm.provider=ollama --ollama.model=llama3.2

Output:
  Discovered universes are saved to ~/.aipublisher/universes/
  Use aipublisher --universe <id> to generate articles from a universe
```

## Best Practices

1. **Start with 3-5 seed topics** - Don't try to enumerate everything upfront
2. **Be specific in descriptions** - Helps AI generate better suggestions
3. **Define scope early** - Prevents AI from suggesting off-topic content
4. **Use the backlog** - Defer interesting but non-essential topics
5. **Review relationships carefully** - They determine generation order
6. **Address critical gaps** - These are often missing prerequisites
7. **Prioritize ruthlessly** - MUST_HAVE should be your core content

## Development

### Running Tests

```bash
# All unit tests
mvn test

# Specific test class
mvn test -Dtest=TopicSuggestionTest
```

### Building

```bash
# Build without tests
mvn clean package -DskipTests

# Build with tests
mvn clean package
```

### Running from Source

```bash
# Using Maven Spring Boot plugin
ANTHROPIC_API_KEY='your-key' mvn spring-boot:run

# With cost profile
ANTHROPIC_API_KEY='your-key' mvn spring-boot:run \
  -Dspring-boot.run.arguments="--cost-profile=minimal"
```

## Technology Stack

- **Java 21** - Modern Java with records, pattern matching
- **Spring Boot 3.3** - Application framework and dependency injection
- **LangChain4j 0.36** - LLM integration framework
- **Picocli 4.7** - Professional CLI framework
- **JUnit 5** - Testing framework

## Related Projects

- **[aipublisher](https://github.com/jakefearsd/aipublisher)** - Article generation from topic universes
  - Generate JSPWiki articles from universes created with aidiscovery
  - Both tools share the `TopicUniverse` JSON format at `~/.aipublisher/universes/`

## License

This project is for educational purposes.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## Acknowledgments

- Built with [Claude](https://claude.ai) by Anthropic
- Uses [LangChain4j](https://github.com/langchain4j/langchain4j) for LLM integration
- CLI powered by [Picocli](https://picocli.info/)
