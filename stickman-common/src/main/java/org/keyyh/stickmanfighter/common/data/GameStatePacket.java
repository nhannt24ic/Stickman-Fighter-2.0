package org.keyyh.stickmanfighter.common.data;

import java.io.Serializable;
import java.util.List;

public class GameStatePacket implements Serializable {
    private static final long serialVersionUID = 1L;

    public List<CharacterState> players;
    public long timestamp;

    public GameStatePacket() {}

    public GameStatePacket(List<CharacterState> players, long timestamp) {
        this.players = players;
        this.timestamp = timestamp;
    }
}