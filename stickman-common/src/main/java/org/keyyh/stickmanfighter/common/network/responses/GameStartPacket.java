package org.keyyh.stickmanfighter.common.network.responses;

import org.keyyh.stickmanfighter.common.data.GameStatePacket;
import java.io.Serializable;

public class GameStartPacket implements Serializable {
    private static final long serialVersionUID = 1L;
    public GameStatePacket initialState; // Gửi kèm trạng thái game đầu tiên của trận đấu

    public GameStartPacket() {}

    public GameStartPacket(GameStatePacket initialState) {
        this.initialState = initialState;
    }
}