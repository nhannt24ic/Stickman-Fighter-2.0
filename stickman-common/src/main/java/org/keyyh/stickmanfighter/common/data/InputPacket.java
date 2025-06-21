package org.keyyh.stickmanfighter.common.data;

import java.io.Serializable;

public class InputPacket implements Serializable {
    private static final long serialVersionUID = 1L;

    public boolean moveLeft;
    public boolean moveRight;
    public boolean jump;

    public InputPacket() {}

    public InputPacket(boolean moveLeft, boolean moveRight, boolean jump) {
        this.moveLeft = moveLeft;
        this.moveRight = moveRight;
        this.jump = jump;
    }
}