package org.keyyh.stickmanfighter.common.data;

import java.io.Serializable;
import java.util.UUID;

public class ConnectionResponsePacket implements Serializable {
    private static final long serialVersionUID = 1L;

    public UUID yourPlayerId;
    public GameStatePacket initialGameState;

    public ConnectionResponsePacket() {}

    public ConnectionResponsePacket(UUID yourPlayerId, GameStatePacket initialGameState) {
        this.yourPlayerId = yourPlayerId;
        this.initialGameState = initialGameState;
    }
}