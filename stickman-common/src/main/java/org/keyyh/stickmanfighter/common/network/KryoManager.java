package org.keyyh.stickmanfighter.common.network;

import com.esotericsoftware.kryo.Kryo;
import org.keyyh.stickmanfighter.common.data.*;

import java.util.ArrayList;
import java.util.UUID;

public class KryoManager {

    public static void register(Kryo kryo) {

        kryo.register(InputPacket.class);
        kryo.register(CharacterState.class);
        kryo.register(Pose.class);
        kryo.register(UUID.class, new UUIDSerializer());
        kryo.register(ConnectionResponsePacket.class);
        kryo.register(ArrayList.class);
        kryo.register(GameStatePacket.class);
    }
}