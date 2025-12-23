package com.jakefear.aidiscovery.cli.curation;

import com.jakefear.aidiscovery.cli.input.ConsoleInputHelper;
import com.jakefear.aidiscovery.discovery.DiscoverySession;

import java.util.function.BiConsumer;

/**
 * Generic command for simple curation actions that just call a session method.
 * Eliminates boilerplate for commands that don't require user interaction.
 *
 * @param <T> The type of suggestion being curated
 */
public class SimpleCurationCommand<T> implements CurationCommand<T> {

    private final CurationAction action;
    private final String resultMessage;
    private final BiConsumer<DiscoverySession, T> sessionAction;

    /**
     * Create a simple curation command.
     *
     * @param action The curation action this command handles
     * @param resultMessage The message to display after execution
     * @param sessionAction The session method to call (e.g., DiscoverySession::acceptTopicSuggestion)
     */
    public SimpleCurationCommand(CurationAction action, String resultMessage,
                                  BiConsumer<DiscoverySession, T> sessionAction) {
        this.action = action;
        this.resultMessage = resultMessage;
        this.sessionAction = sessionAction;
    }

    @Override
    public CurationResult execute(T suggestion, DiscoverySession session,
                                  ConsoleInputHelper input) {
        sessionAction.accept(session, suggestion);
        return CurationResult.success(resultMessage);
    }

    @Override
    public CurationAction getAction() {
        return action;
    }
}
