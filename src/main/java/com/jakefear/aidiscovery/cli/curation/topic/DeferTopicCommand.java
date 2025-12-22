package com.jakefear.aidiscovery.cli.curation.topic;

import com.jakefear.aidiscovery.cli.curation.CurationAction;
import com.jakefear.aidiscovery.cli.curation.CurationCommand;
import com.jakefear.aidiscovery.cli.curation.CurationResult;
import com.jakefear.aidiscovery.cli.input.ConsoleInputHelper;
import com.jakefear.aidiscovery.discovery.DiscoverySession;
import com.jakefear.aidiscovery.discovery.TopicSuggestion;

/**
 * Command to defer a topic suggestion to backlog.
 */
public class DeferTopicCommand implements CurationCommand<TopicSuggestion> {

    @Override
    public CurationResult execute(TopicSuggestion suggestion, DiscoverySession session,
                                  ConsoleInputHelper input) {
        session.deferTopicSuggestion(suggestion);
        return CurationResult.success("Deferred to backlog");
    }

    @Override
    public CurationAction getAction() {
        return CurationAction.DEFER;
    }
}
