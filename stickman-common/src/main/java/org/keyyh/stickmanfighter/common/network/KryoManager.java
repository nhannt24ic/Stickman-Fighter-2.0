package org.keyyh.stickmanfighter.common.network;

import com.esotericsoftware.kryo.Kryo;
import org.keyyh.stickmanfighter.common.data.*;
import org.keyyh.stickmanfighter.common.enums.ActionModifier;
import org.keyyh.stickmanfighter.common.enums.CharacterFSMState;
import org.keyyh.stickmanfighter.common.enums.PlayerAction;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

public class KryoManager {

    public static void register(Kryo kryo) {
        kryo.register(InputPacket.class, new InputPacketSerializer());
        kryo.register(CharacterState.class);
        kryo.register(Pose.class);
        kryo.register(UUID.class, new UUIDSerializer());
        kryo.register(ConnectionResponsePacket.class);
        kryo.register(ArrayList.class);
        kryo.register(GameStatePacket.class);
        kryo.register(PlayerAction.class);
        kryo.register(ActionModifier.class);
        kryo.register(CharacterFSMState.class);
        kryo.register(HashSet.class);
        kryo.register(Rectangle.class);
    }
}