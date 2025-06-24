package org.keyyh.stickmanfighter.common.network.responses;

import java.io.Serializable;
import java.util.UUID;

public class RoomInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    public UUID roomId;
    public String roomName;
    public int playerCount;
    public final int MAX_PLAYERS = 2;

    public RoomInfo() {}

    public RoomInfo(UUID roomId, String roomName, int playerCount) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.playerCount = playerCount;
    }
}