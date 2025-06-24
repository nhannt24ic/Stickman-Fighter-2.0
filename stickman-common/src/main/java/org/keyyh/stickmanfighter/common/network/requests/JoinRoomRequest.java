package org.keyyh.stickmanfighter.common.network.requests;

import java.io.Serializable;
import java.util.UUID;

public class JoinRoomRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    public UUID roomId;

    public JoinRoomRequest() {}

    public JoinRoomRequest(UUID roomId) {
        this.roomId = roomId;
    }
}