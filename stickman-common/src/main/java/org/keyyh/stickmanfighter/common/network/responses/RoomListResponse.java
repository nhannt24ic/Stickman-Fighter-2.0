package org.keyyh.stickmanfighter.common.network.responses;

import java.io.Serializable;
import java.util.List;

public class RoomListResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    public List<RoomInfo> rooms;

    public RoomListResponse() {}

    public RoomListResponse(List<RoomInfo> rooms) {
        this.rooms = rooms;
    }
}