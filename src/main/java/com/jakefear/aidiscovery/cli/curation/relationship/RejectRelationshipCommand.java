package com.jakefear.aidiscovery.cli.curation.relationship;

import com.jakefear.aidiscovery.cli.curation.CurationAction;
import com.jakefear.aidiscovery.cli.curation.CurationCommand;
import com.jakefear.aidiscovery.cli.curation.CurationResult;
import com.jakefear.aidiscovery.cli.input.ConsoleInputHelper;
import com.jakefear.aidiscovery.discovery.DiscoverySession;
import com.jakefear.aidiscovery.discovery.RelationshipSuggestion;

/**
 * Command to reject a relationship suggestion.
 */
public class RejectRelationshipCommand implements CurationCommand<RelationshipSuggestion> {

    @Override
    public CurationResult execute(RelationshipSuggestion suggestion, DiscoverySession session,
                                  ConsoleInputHelper input) {
        session.rejectRelationship(suggestion);
        return CurationResult.success("Rejected");
    }

    @Override
    public CurationAction getAction() {
        return CurationAction.REJECT;
    }
}
