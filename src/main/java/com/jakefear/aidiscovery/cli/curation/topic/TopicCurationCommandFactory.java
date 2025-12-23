package com.jakefear.aidiscovery.cli.curation.topic;

import com.jakefear.aidiscovery.cli.curation.CurationAction;
import com.jakefear.aidiscovery.cli.curation.CurationCommand;
import com.jakefear.aidiscovery.cli.curation.SimpleCurationCommand;
import com.jakefear.aidiscovery.discovery.DiscoverySession;
import com.jakefear.aidiscovery.discovery.TopicSuggestion;

import java.util.List;
import java.util.Optional;

/**
 * Factory for creating topic curation commands based on user action.
 */
public class TopicCurationCommandFactory {

    private final CurationCommand<TopicSuggestion> acceptCommand = new SimpleCurationCommand<>(
            CurationAction.ACCEPT, "Accepted", DiscoverySession::acceptTopicSuggestion);
    private final CurationCommand<TopicSuggestion> rejectCommand = new SimpleCurationCommand<>(
            CurationAction.REJECT, "Rejected", DiscoverySession::rejectTopicSuggestion);
    private final CurationCommand<TopicSuggestion> deferCommand = new SimpleCurationCommand<>(
            CurationAction.DEFER, "Deferred to backlog", DiscoverySession::deferTopicSuggestion);
    private final ModifyTopicCommand modifyCommand = new ModifyTopicCommand();

    /**
     * Get a command for the given action.
     *
     * @param action The user's action
     * @param remainingSuggestions Remaining suggestions (for skip rest)
     * @return The command if recognized, empty for quit/unrecognized actions
     */
    public Optional<CurationCommand<TopicSuggestion>> getCommand(
            CurationAction action,
            List<TopicSuggestion> remainingSuggestions) {

        if (action == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(switch (action) {
            case ACCEPT, DEFAULT -> acceptCommand;
            case REJECT -> rejectCommand;
            case DEFER -> deferCommand;
            case MODIFY -> modifyCommand;
            case SKIP_REST -> new SkipRestTopicCommand(remainingSuggestions);
            default -> null;
        });
    }

    /**
     * Get the menu prompt for topic curation.
     */
    public String getMenuPrompt() {
        return "[A]ccept  [R]eject  [D]efer  [M]odify  [S]kip rest  [Q]uit";
    }
}
