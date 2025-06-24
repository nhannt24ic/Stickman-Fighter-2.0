package org.keyyh.stickmanfighter.common.data;

import org.keyyh.stickmanfighter.common.enums.CharacterFSMState;
import java.awt.geom.Line2D;
import java.io.Serializable;
import java.util.UUID;

public class CharacterState implements Serializable {
    private static final long serialVersionUID = 1L;

    public UUID id;
    public double x, y;
    public Pose currentPose;
    public boolean isFacingRight;
    public CharacterFSMState fsmState;
    public Line2D.Double activeHitbox;

    public double health;
    public double stamina;

    public CharacterState() {}

    public CharacterState(UUID id, double x, double y, Pose currentPose, boolean isFacingRight, CharacterFSMState fsmState, Line2D.Double activeHitbox, double health, double stamina) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.currentPose = currentPose;
        this.isFacingRight = isFacingRight;
        this.fsmState = fsmState;
        this.activeHitbox = activeHitbox;
        this.health = health;
        this.stamina = stamina;
    }
}