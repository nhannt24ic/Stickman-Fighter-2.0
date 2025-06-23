package org.keyyh.stickmanfighter.common.data;

import org.keyyh.stickmanfighter.common.enums.ActionModifier;
import org.keyyh.stickmanfighter.common.enums.PlayerAction;

import java.io.Serializable;
import java.util.Set;

public class InputPacket implements Serializable {
    private static final long serialVersionUID = 1L;

    public PlayerAction primaryAction;
    public Set<ActionModifier> modifiers;

    public InputPacket() {}

    public InputPacket(PlayerAction primaryAction, Set<ActionModifier> modifiers) {
        this.primaryAction = primaryAction;
        this.modifiers = modifiers;
    }
}