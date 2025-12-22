package com.jakefear.aidiscovery.cli.curation.topic;

import com.jakefear.aidiscovery.cli.curation.CurationAction;
import com.jakefear.aidiscovery.cli.curation.CurationCommand;
import com.jakefear.aidiscovery.cli.curation.CurationResult;
import com.jakefear.aidiscovery.cli.input.ConsoleInputHelper;
import com.jakefear.aidiscovery.discovery.DiscoverySession;
import com.jakefear.aidiscovery.discovery.TopicSuggestion;

/**
 * Command to accept a topic suggestion as-is.
 */
public class AcceptTopicCommand implements CurationCommand<TopicSuggestion> {

    @Override
    public CurationResult execute(TopicSuggestion suggestion, DiscoverySession session,
                                  ConsoleInputHelper input) {
        session.acceptTopicSuggestion(suggestion);
        return CurationResult.success("Accepted");
    }

    @Override
    public CurationAction getAction() {
        return CurationAction.ACCEPT;
    }
}
