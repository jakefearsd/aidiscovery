package com.jakefear.aidiscovery.cli.curation.relationship;

import com.jakefear.aidiscovery.cli.curation.CurationAction;
import com.jakefear.aidiscovery.cli.curation.CurationCommand;
import com.jakefear.aidiscovery.cli.curation.SimpleCurationCommand;
import com.jakefear.aidiscovery.discovery.DiscoverySession;
import com.jakefear.aidiscovery.discovery.RelationshipSuggestion;

import java.util.Optional;

/**
 * Factory for creating relationship curation commands based on user action.
 */
public class RelationshipCurationCommandFactory {

    private final CurationCommand<RelationshipSuggestion> confirmCommand = new SimpleCurationCommand<>(
            CurationAction.CONFIRM, "Confirmed", DiscoverySession::confirmRelationship);
    private final CurationCommand<RelationshipSuggestion> rejectCommand = new SimpleCurationCommand<>(
            CurationAction.REJECT, "Rejected", DiscoverySession::rejectRelationship);
    private final ChangeTypeRelationshipCommand changeTypeCommand = new ChangeTypeRelationshipCommand();

    /**
     * Get a command for the given action.
     *
     * @param action The user's action
     * @return The command if recognized, empty for quit/unrecognized actions
     */
    public Optional<CurationCommand<RelationshipSuggestion>> getCommand(CurationAction action) {
        if (action == null) {
            return Optional.of(confirmCommand); // Default to confirm for unrecognized input
        }
        return Optional.ofNullable(switch (action) {
            case CONFIRM, DEFAULT -> confirmCommand;
            case REJECT -> rejectCommand;
            case TYPE_CHANGE -> changeTypeCommand;
            default -> null;
        });
    }

    /**
     * Get the menu prompt for relationship curation.
     */
    public String getMenuPrompt() {
        return "[C]onfirm  [R]eject  [T]ype change";
    }
}
