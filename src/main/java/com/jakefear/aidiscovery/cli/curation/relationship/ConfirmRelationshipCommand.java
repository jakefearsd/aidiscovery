package com.jakefear.aidiscovery.cli.curation.relationship;

import com.jakefear.aidiscovery.cli.curation.CurationAction;
import com.jakefear.aidiscovery.cli.curation.CurationCommand;
import com.jakefear.aidiscovery.cli.curation.CurationResult;
import com.jakefear.aidiscovery.cli.input.ConsoleInputHelper;
import com.jakefear.aidiscovery.discovery.DiscoverySession;
import com.jakefear.aidiscovery.discovery.RelationshipSuggestion;

/**
 * Command to confirm a relationship suggestion.
 */
public class ConfirmRelationshipCommand implements CurationCommand<RelationshipSuggestion> {

    @Override
    public CurationResult execute(RelationshipSuggestion suggestion, DiscoverySession session,
                                  ConsoleInputHelper input) {
        session.confirmRelationship(suggestion);
        return CurationResult.success("Confirmed");
    }

    @Override
    public CurationAction getAction() {
        return CurationAction.CONFIRM;
    }
}
