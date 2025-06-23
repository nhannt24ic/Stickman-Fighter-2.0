package org.keyyh.stickmanfighter.common.data;

import org.keyyh.stickmanfighter.common.enums.CharacterFSMState;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.UUID;

public class CharacterState implements Serializable {
    private static final long serialVersionUID = 1L;

    public UUID id;
    public double x;
    public double y;
    public Pose currentPose;
    public boolean isFacingRight;
    public CharacterFSMState fsmState;
    public Rectangle activeHitbox;

    public CharacterState() {}

    public CharacterState(UUID id, double x, double y, Pose currentPose, boolean isFacingRight, CharacterFSMState fsmState, Rectangle activeHitbox) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.currentPose = currentPose;
        this.isFacingRight = isFacingRight;
        this.fsmState = fsmState;
        this.activeHitbox = activeHitbox;
    }
}