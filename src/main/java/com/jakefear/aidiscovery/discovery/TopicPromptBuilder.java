package com.jakefear.aidiscovery.discovery;

import com.jakefear.aidiscovery.domain.ScopeConfiguration;
import com.jakefear.aidiscovery.search.SearchResult;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builder for constructing topic expansion prompts.
 * Eliminates duplication across different prompt types by providing
 * composable building blocks for prompt sections.
 */
public class TopicPromptBuilder {

    private final StringBuilder prompt = new StringBuilder();

    /**
     * Create a new prompt builder.
     */
    public static TopicPromptBuilder create() {
        return new TopicPromptBuilder();
    }

    /**
     * Add the domain context section.
     *
     * @param domainName The name of the domain
     * @return this builder for chaining
     */
    public TopicPromptBuilder addDomainContext(String domainName) {
        prompt.append("## Domain Context\n");
        prompt.append("Domain: ").append(domainName).append("\n\n");
        return this;
    }

    /**
     * Add the domain section with optional description (for initial topics prompt).
     *
     * @param domainName The name of the domain
     * @param description Optional domain description
     * @return this builder for chaining
     */
    public TopicPromptBuilder addDomainWithDescription(String domainName, String description) {
        prompt.append("## Domain\n");
        prompt.append("Name: ").append(domainName).append("\n");
        if (description != null && !description.isBlank()) {
            prompt.append("Description: ").append(description).append("\n");
        }
        prompt.append("\n");
        return this;
    }

    /**
     * Add the seed topic section.
     *
     * @param seedTopic The topic to expand from
     * @return this builder for chaining
     */
    public TopicPromptBuilder addSeedTopic(String seedTopic) {
        prompt.append("## Seed Topic to Expand\n");
        prompt.append("Topic: ").append(seedTopic).append("\n");
        return this;
    }

    /**
     * Add search context for grounding suggestions in real knowledge.
     *
     * @param topicInfo Search result with topic summary (may be null)
     * @param relatedTopics Related topics from search
     * @return this builder for chaining
     */
    public TopicPromptBuilder addSearchContext(SearchResult topicInfo, List<String> relatedTopics) {
        if (topicInfo != null) {
            prompt.append("Summary from search: ").append(topicInfo.snippet()).append("\n");
        }

        if (relatedTopics != null && !relatedTopics.isEmpty()) {
            prompt.append("Related topics from search: ");
            prompt.append(String.join(", ", relatedTopics.subList(0, Math.min(15, relatedTopics.size()))));
            prompt.append("\n");
        }
        prompt.append("\n");
        return this;
    }

    /**
     * Add the existing topics section to prevent duplicates.
     *
     * @param existingTopics Set of existing topic names
     * @return this builder for chaining
     */
    public TopicPromptBuilder addExistingTopics(Set<String> existingTopics) {
        if (existingTopics != null && !existingTopics.isEmpty()) {
            prompt.append("## Existing Topics (do not suggest duplicates)\n");
            prompt.append(existingTopics.stream()
                    .sorted()
                    .collect(Collectors.joining(", ")));
            prompt.append("\n\n");
        }
        return this;
    }

    /**
     * Add scope guidance section.
     *
     * @param scope The scope configuration (may be null)
     * @return this builder for chaining
     */
    public TopicPromptBuilder addScopeGuidance(ScopeConfiguration scope) {
        if (scope == null) {
            return this;
        }

        prompt.append("## Scope Guidance\n");

        if (!scope.assumedKnowledge().isEmpty()) {
            prompt.append("Assumed reader knowledge (do not cover): ");
            prompt.append(String.join(", ", scope.assumedKnowledge()));
            prompt.append("\n");
        }

        if (!scope.outOfScope().isEmpty()) {
            prompt.append("Out of scope (exclude): ");
            prompt.append(String.join(", ", scope.outOfScope()));
            prompt.append("\n");
        }

        if (!scope.focusAreas().isEmpty()) {
            prompt.append("Focus areas (prioritize): ");
            prompt.append(String.join(", ", scope.focusAreas()));
            prompt.append("\n");
        }

        if (scope.audienceDescription() != null && !scope.audienceDescription().isBlank()) {
            prompt.append("Target audience: ").append(scope.audienceDescription());
            prompt.append("\n");
        }

        prompt.append("\n");
        return this;
    }

    /**
     * Add task section for search-grounded expansion.
     *
     * @param suggestionsRange Range description (e.g., "3-5")
     * @return this builder for chaining
     */
    public TopicPromptBuilder addSearchGroundedTask(String suggestionsRange) {
        prompt.append(String.format("""
            ## Task
            Analyze the seed topic and suggest %s related topics that would help create a comprehensive wiki.
            Focus on topics that directly support understanding or applying the seed topic.

            IMPORTANT: Prefer suggesting topics that appear in the "Related topics from search" list or are
            closely related to them. This helps ensure topic suggestions are grounded in real knowledge.

            Respond with JSON in this format:
            ```json
            {
              "suggestions": [
                {
                  "name": "Topic Name",
                  "description": "Brief description of what this topic covers",
                  "category": "prerequisite|component|related|application|advanced",
                  "contentType": "CONCEPT|TUTORIAL|REFERENCE|HOW_TO|COMPARISON|TROUBLESHOOTING",
                  "complexity": "BEGINNER|INTERMEDIATE|ADVANCED",
                  "relevance": 0.85,
                  "rationale": "Why this topic is important for the wiki"
                }
              ]
            }
            ```
            """, suggestionsRange));
        return this;
    }

    /**
     * Add task section for standard expansion (without search grounding).
     *
     * @param suggestionsRange Range description (e.g., "3-5")
     * @return this builder for chaining
     */
    public TopicPromptBuilder addStandardTask(String suggestionsRange) {
        prompt.append(String.format("""
            ## Task
            Analyze the seed topic and suggest %s related topics that would help create a comprehensive wiki.
            Focus on topics that directly support understanding or applying the seed topic.

            Respond with JSON in this format:
            ```json
            {
              "suggestions": [
                {
                  "name": "Topic Name",
                  "description": "Brief description of what this topic covers",
                  "category": "prerequisite|component|related|application|advanced",
                  "contentType": "CONCEPT|TUTORIAL|REFERENCE|HOW_TO|COMPARISON|TROUBLESHOOTING",
                  "complexity": "BEGINNER|INTERMEDIATE|ADVANCED",
                  "relevance": 0.85,
                  "rationale": "Why this topic is important for the wiki"
                }
              ]
            }
            ```
            """, suggestionsRange));
        return this;
    }

    /**
     * Add task section for initial domain analysis.
     *
     * @return this builder for chaining
     */
    public TopicPromptBuilder addInitialTopicsTask() {
        prompt.append("""
            ## Task
            Analyze this domain and suggest 10-15 foundational topics that would form the core of a comprehensive wiki.
            Include a mix of:
            - Core concepts that define the domain
            - Practical tutorials for hands-on learning
            - Reference material for ongoing use
            - Comparisons with alternatives where relevant

            Respond with JSON in this format:
            ```json
            {
              "suggestions": [
                {
                  "name": "Topic Name",
                  "description": "Brief description of what this topic covers",
                  "category": "core|foundation|practical|reference|comparison",
                  "contentType": "CONCEPT|TUTORIAL|REFERENCE|HOW_TO|COMPARISON|TROUBLESHOOTING",
                  "complexity": "BEGINNER|INTERMEDIATE|ADVANCED",
                  "relevance": 0.85,
                  "rationale": "Why this topic is essential for the wiki"
                }
              ]
            }
            ```
            """);
        return this;
    }

    /**
     * Add a newline separator.
     *
     * @return this builder for chaining
     */
    public TopicPromptBuilder addNewline() {
        prompt.append("\n");
        return this;
    }

    /**
     * Build the final prompt string.
     *
     * @return the constructed prompt
     */
    public String build() {
        return prompt.toString();
    }
}
