# AI Discovery

A CLI tool for building comprehensive topic universes through AI-assisted domain discovery. Use AI to map out your knowledge domain, curate topics, define relationships, and identify gaps before generating content.

## Two Discovery Modes

| Mode | Command | Best For |
|------|---------|----------|
| **Autonomous** | `./ifeellucky.sh "Domain" "Description"` | Quick generation, exploration, prototyping |
| **Interactive** | `java -jar target/aidiscovery.jar` | Full control, enterprise documentation |

Both modes produce the same output: a `TopicUniverse` JSON file ready for content generation with [aipublisher](https://github.com/jakefearsd/aipublisher).

---

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
```

### 3. Generate a Topic Universe

**Autonomous Mode (Recommended for first-time users):**
```bash
# Using the convenience script (uses local Ollama)
./ifeellucky.sh "Kubernetes" "Admin guide for DevOps engineers"

# Using java directly with Anthropic
java -jar target/aidiscovery.jar -L "Machine Learning" -d "Python developers guide"
```

**Interactive Mode:**
```bash
java -jar target/aidiscovery.jar -c balanced
```

---

## Autonomous Mode ("I Feel Lucky")

Fully automated discovery where AI handles all curation decisions. Give it a domain and description, and it produces a complete topic universe.

### Using the Convenience Script

The `ifeellucky.sh` script is pre-configured for local Ollama inference:

```bash
# Basic usage
./ifeellucky.sh "Domain Name" "Description of wiki goals and audience"

# Examples
./ifeellucky.sh "Kubernetes" "Admin guide for DevOps engineers"
./ifeellucky.sh "Python Type Hints" "For intermediate developers adding types to existing code"
./ifeellucky.sh "Berlin History" "Cultural and political history for tourists"

# Override cost profile (default: comprehensive)
./ifeellucky.sh -c balanced "Machine Learning" "Intro for Python developers"
./ifeellucky.sh -c minimal "Quick Topic" "Just testing"
```

### Direct Java Usage

For more control or when using Anthropic:

```bash
# With Anthropic (requires ANTHROPIC_API_KEY)
java -jar target/aidiscovery.jar \
  --ifeellucky "Cloud Security" \
  -d "AWS architects guide to security best practices" \
  -c balanced

# With Ollama (local)
java -jar target/aidiscovery.jar \
  --ifeellucky "React Hooks" \
  -d "For developers migrating from class components" \
  -c comprehensive \
  --llm.provider=ollama

# With confirmation step (review scope before proceeding)
java -jar target/aidiscovery.jar \
  -L "Database Design" \
  -d "SQL fundamentals for backend developers" \
  --confirm

# Dry run (show plan without executing)
java -jar target/aidiscovery.jar \
  -L "API Design" \
  -d "REST best practices" \
  --dry-run
```

### Autonomous CLI Options

| Option | Short | Description |
|--------|-------|-------------|
| `--ifeellucky <domain>` | `-L` | Activate autonomous mode with domain name |
| `--description <text>` | `-d` | Wiki goals, audience, focus (strongly recommended) |
| `--confirm` | | Pause after scope inference for user approval |
| `--dry-run` | | Show plan without executing |

### How Autonomous Mode Works

The AI executes a 6-phase workflow:

```
[1/6] Inferring scope from description...
  Audience: DevOps engineers with Linux/Docker background
  Focus: Production administration, troubleshooting
  Seeds: Kubernetes Architecture, Pod Management, Deployments

[2/6] Expanding topics (round 1/3)...
  + Kubernetes Architecture (0.92) - auto-accepted
  + Pod Lifecycle (0.88) - auto-accepted
  + ConfigMaps and Secrets (0.76) - AI-verified, accepted
  - Kubernetes History (0.35) - auto-rejected

[3/6] Mapping relationships...
  12 relationships confirmed (8 auto, 4 AI-verified)

[4/6] Analyzing coverage gaps...
  + Added "Troubleshooting Pods" (critical gap)

[5/6] Assigning priorities...
  MUST_HAVE: 5 topics (landing + prerequisites)
  SHOULD_HAVE: 12 topics

[6/6] Finalizing...

Complete! Saved to ~/.aipublisher/universes/kubernetes-admin.universe.json
  Topics:   17 accepted
  Words:    ~32,000 estimated
```

### AI Decision Algorithm

The autonomous curator uses combined scores to decide:

```
combined_score = (relevance * 0.6) + (search_confidence * 0.4)

AUTO_ACCEPT if score >= 0.75 AND search_confidence >= 0.3
AUTO_REJECT if score < 0.4 OR search_confidence < 0.2
ASK_AI     otherwise (borderline cases get LLM reasoning)
```

- **Relevance score** - LLM's assessment of topic relevance to domain
- **Search confidence** - Wikidata/Wikipedia validation (prevents hallucinated topics)

---

## Interactive Mode (8 Phases)

Full human-in-the-loop discovery for maximum control. You curate every topic and relationship.

```bash
java -jar target/aidiscovery.jar -c balanced
```

### Phase Overview

| Phase | Description | Your Actions |
|-------|-------------|--------------|
| 1. Seed Input | Provide domain and initial topics | Name domain, add seeds, select landing page |
| 2. Scope Setup | Define boundaries | Set assumed knowledge, exclusions, focus |
| 3. Topic Expansion | AI suggests related topics | Accept, Reject, Defer, Modify each |
| 4. Relationship Mapping | Define how topics connect | Confirm, Reject, Change relationship type |
| 5. Gap Analysis | AI identifies missing content | Review and add suggested topics |
| 6. Depth Calibration | Adjust word counts | Modify estimates per topic |
| 7. Prioritization | Set generation order | Assign MUST/SHOULD/NICE_TO_HAVE |
| 8. Review | Final approval | Confirm and save |

### Phase 1: Seed Input

```
What domain or subject area is this wiki about?
Domain name: Apache Kafka

Enter your initial seed topics (one per line, empty line to finish):
Seed topic 1: Kafka Producers
  Brief description: How to send messages to Kafka
Seed topic 2: Kafka Consumers
  Brief description: How to read messages from Kafka
Seed topic 3:

Which topic should be the main landing page? [1]:
```

### Phase 2: Scope Setup

```
What knowledge should readers already have? (comma-separated)
Assumed knowledge: Java programming, basic distributed systems

What topics should be explicitly excluded? (comma-separated)
Out of scope: Kafka Streams, Kafka Connect

Any specific areas to prioritize? (comma-separated)
Focus areas: Production deployment, Performance tuning

Target audience description: Backend developers new to event streaming
```

### Phase 3: Topic Expansion

AI suggests topics based on your seeds and scope. For each suggestion:

```
┌──────────────────────────────────────────────────────────┐
│ 1/7: Producer Configuration                              │
├──────────────────────────────────────────────────────────┤
│ Essential settings for Kafka producers including acks,   │
│ retries, batch size, and compression options.            │
│ Relevance: ████████░░ 80%   Search: ███████░░░ 70%       │
│ Type: Reference               Complexity: Intermediate   │
└──────────────────────────────────────────────────────────┘

  [A]ccept  [R]eject  [D]efer  [M]odify  [S]kip rest
  Decision: a
```

**Curation Options:**
- **Accept** - Add topic to universe as-is
- **Reject** - Skip this topic entirely
- **Defer** - Save to backlog for later
- **Modify** - Change name/description before accepting
- **Skip rest** - Auto-accept high-relevance, defer low-relevance

### Phase 4: Relationship Mapping

AI suggests relationships between topics:

```
● Kafka Basics ──[PREREQUISITE_OF]──> Kafka Producers
  └─ Understanding core concepts is essential before producing
  [C]onfirm  [R]eject  [T]ype change: c
```

**Relationship Types:**

| Type | Meaning | Example |
|------|---------|---------|
| `PREREQUISITE_OF` | Must understand A before B | Java → Spring Boot |
| `PART_OF` | A is a component of B | Partitions → Topics |
| `RELATED_TO` | Loosely connected | Producers ↔ Consumers |
| `EXAMPLE_OF` | A is an instance of B | Avro → Serialization |
| `CONTRASTS_WITH` | Alternatives | Kafka vs RabbitMQ |
| `IMPLEMENTS` | A implements B | KafkaProducer → Producer API |
| `SUPERSEDES` | A replaces B | New API → Legacy API |
| `PAIRS_WITH` | Commonly used together | Producers + Schema Registry |

### Phase 5: Gap Analysis

AI analyzes coverage and suggests missing topics:

```
Coverage Assessment:
  Coverage:      ████████░░ 80%
  Balance:       ███████░░░ 70%
  Connectedness: █████████░ 90%

Found 3 gaps to review:

🔴 [MISSING_PREREQUISITE] Consumer Group Coordination
   Add suggested topic? [Y/n]: y
```

**Gap Severity:**
- 🔴 **Critical** - Missing prerequisite or core concept
- 🟡 **Moderate** - Notable gap that should be addressed
- 🟢 **Minor** - Nice to have

### Phase 6-8: Calibration, Prioritization, Review

Fine-tune word counts, assign priorities (MUST_HAVE, SHOULD_HAVE, NICE_TO_HAVE), and review before saving.

---

## Search Grounding

AI Discovery validates all topic suggestions against knowledge bases to prevent hallucinated topics.

### How It Works

1. LLM suggests a topic (e.g., "Kubernetes Pod Lifecycle")
2. System queries Wikidata/Wikipedia for validation
3. Search confidence score (0.0-1.0) is calculated
4. Low-confidence topics are flagged or rejected

### Validation Scoring

| Match Type | Confidence |
|------------|------------|
| Exact match | 1.0 |
| Contains match | 0.85 |
| Word overlap (proportional) | 0.5-0.85 |
| Partial words | 0.35-0.6 |
| No match | 0.0 |

### Provider Selection

By default, Wikidata is used (more lenient, searches labels AND aliases). Wikipedia is available as a fallback.

---

## Cost Profiles

Control how thoroughly AI explores your domain.

| Profile | Topics | Rounds | Est. Cost | Best For |
|---------|--------|--------|-----------|----------|
| **MINIMAL** | 2-4 | 1 | $0.50-2 | Prototyping, testing |
| **BALANCED** | 9-31 | 3 | $2-5 | Most projects |
| **COMPREHENSIVE** | 25-150 | 5 | $5-15 | Enterprise docs |

**Detailed Settings:**

| Setting | MINIMAL | BALANCED | COMPREHENSIVE |
|---------|---------|----------|---------------|
| Max expansion rounds | 1 | 3 | 5 |
| Topics per round | 2 | 3 | 5 |
| Suggestions per topic | 2-6 | 5-9 | 10-14 |
| Max complexity | Intermediate | Advanced | Expert |
| Word count multiplier | 0.6x | 1.0x | 1.5x |
| Gap analysis | Skipped | Enabled | Enabled |
| Relationship depth | Core only | Important | All |

---

## Command Line Reference

```
Usage: aidiscovery [OPTIONS]

Discovery Modes:
  (default)                       Interactive 8-phase discovery
  -L, --ifeellucky <domain>       Autonomous "I Feel Lucky" mode
  -d, --description <text>        Wiki goals/audience for autonomous mode

Cost & Resource Control:
  -c, --cost-profile <profile>    MINIMAL, BALANCED, or COMPREHENSIVE
      --confirm                   Pause after scope inference (autonomous)
      --dry-run                   Show plan without executing (autonomous)

Universe Management:
      --list                      List all saved universes
      --show <id>                 Show universe details
      --export <id> <path>        Export universe to file

LLM Provider:
      --llm.provider=<provider>   "anthropic" or "ollama" (default: anthropic)
      --ollama.base-url=<url>     Ollama server (default: http://localhost:11434)
      --ollama.model=<model>      Ollama model (default: qwen3:14b)
      --anthropic.model=<model>   Anthropic model (default: claude-sonnet-4-20250514)

API Key (Anthropic only):
  -k, --key=<key>                 API key (overrides environment)
      --key-file=<path>           Path to file containing API key
      ANTHROPIC_API_KEY           Environment variable (default)

Output:
  -v, --verbose                   Enable verbose output
  -q, --quiet                     Suppress non-essential output
  -h, --help                      Show help
  -V, --version                   Show version
```

### Examples

```bash
# Autonomous mode - quick discovery
./ifeellucky.sh "Kubernetes" "Admin guide for DevOps engineers"
java -jar target/aidiscovery.jar -L "React Hooks" -d "For class component migrants" -c balanced

# Interactive mode - full control
java -jar target/aidiscovery.jar -c comprehensive

# Local inference with Ollama
java -jar target/aidiscovery.jar --llm.provider=ollama
java -jar target/aidiscovery.jar --llm.provider=ollama --ollama.model=llama3.2

# Manage saved universes
java -jar target/aidiscovery.jar --list
java -jar target/aidiscovery.jar --show kubernetes-admin
java -jar target/aidiscovery.jar --export my-wiki ./backup.json
```

---

## Output Format

Topic universes are saved to `~/.aipublisher/universes/<id>.universe.json`:

```json
{
  "id": "kafka-wiki-2024",
  "name": "Apache Kafka",
  "topics": [
    {
      "id": "KafkaProducers",
      "name": "Kafka Producers",
      "status": "ACCEPTED",
      "priority": "MUST_HAVE",
      "contentType": "TUTORIAL",
      "complexity": "INTERMEDIATE",
      "estimatedWords": 1500,
      "isLandingPage": true
    }
  ],
  "relationships": [
    {
      "source": "KafkaBasics",
      "target": "KafkaProducers",
      "type": "PREREQUISITE_OF",
      "status": "CONFIRMED"
    }
  ],
  "scope": {
    "assumedKnowledge": ["Java programming"],
    "outOfScope": ["Kafka Streams"],
    "focusAreas": ["Production deployment"],
    "audienceDescription": "Backend developers new to event streaming"
  }
}
```

Use with aipublisher to generate articles:
```bash
aipublisher --universe kafka-wiki-2024
```

---

## Best Practices

### For Autonomous Mode
1. **Write descriptive descriptions** - The AI infers scope from your description
2. **Use balanced profile for exploration** - Comprehensive can over-expand
3. **Review the output** - Edit the JSON file to refine before generation

### For Interactive Mode
1. **Start with 3-5 seed topics** - Let AI discover the rest
2. **Define scope early** - Prevents off-topic suggestions
3. **Use the backlog** - Defer interesting but non-essential topics
4. **Review relationships carefully** - They determine generation order
5. **Address critical gaps** - These are often missing prerequisites

### General
- **Cost profiles matter** - Start with MINIMAL or BALANCED to understand output
- **Local inference is free** - Use Ollama for exploration, Anthropic for final runs
- **Universes are editable** - The JSON file can be manually refined

---

## Development

### Building

```bash
mvn clean package -DskipTests  # Quick build
mvn clean package              # With tests
```

### Running from Source

```bash
ANTHROPIC_API_KEY='key' mvn spring-boot:run
mvn spring-boot:run -Dspring-boot.run.arguments="--llm.provider=ollama"
```

### Running Tests

```bash
mvn test
mvn test -Dtest=TopicExpanderTest
```

---

## Architecture

For internal design details, see [ARCHITECTURE.md](ARCHITECTURE.md):
- System overview and package structure
- Core components (TopicExpander, AutonomousCurator, etc.)
- Discovery modes and phase flows
- Search grounding implementation
- AI integration and prompt engineering
- Extension points

---

## Technology Stack

- **Java 21** - Modern Java with records, pattern matching
- **Spring Boot 3.3** - Application framework
- **LangChain4j 0.36** - LLM integration
- **Picocli 4.7** - CLI framework
- **JUnit 5** - Testing

---

## Related Projects

- **[aipublisher](https://github.com/jakefearsd/aipublisher)** - Generate JSPWiki articles from topic universes

---

## License

This project is for educational purposes.

## Acknowledgments

- Built with [Claude](https://claude.ai) by Anthropic
- Uses [LangChain4j](https://github.com/langchain4j/langchain4j) for LLM integration
- CLI powered by [Picocli](https://picocli.info/)
