package org.keyyh.stickmanfighter.common.data;

import java.io.Serializable;
import java.util.UUID;

public class CharacterState implements Serializable {
    private static final long serialVersionUID = 1L;

    public UUID id;
    public double x;
    public double y;
    public Pose currentPose;
    public boolean isFacingRight;

    public CharacterState() {}

    public CharacterState(UUID id, double x, double y, Pose currentPose, boolean isFacingRight) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.currentPose = currentPose;
        this.isFacingRight = isFacingRight;
    }
}