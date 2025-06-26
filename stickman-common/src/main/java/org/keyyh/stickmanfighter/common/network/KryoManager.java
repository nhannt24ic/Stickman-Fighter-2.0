package org.keyyh.stickmanfighter.common.network;

import com.esotericsoftware.kryo.Kryo;
import org.keyyh.stickmanfighter.common.data.*;
import org.keyyh.stickmanfighter.common.enums.*;
import org.keyyh.stickmanfighter.common.network.requests.FindMatchRequest;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

public class KryoManager {

    public static void register(Kryo kryo) {
        kryo.setRegistrationRequired(false);

        kryo.register(InputPacket.class, new InputPacketSerializer());
        kryo.register(CharacterState.class);
        kryo.register(Pose.class);
        kryo.register(UUID.class, new UUIDSerializer());
        kryo.register(ConnectionResponsePacket.class);
        kryo.register(GameStatePacket.class, new GameStatePacketSerializer());

        kryo.register(PlayerAction.class);
        kryo.register(ActionModifier.class);
        kryo.register(CharacterFSMState.class);

        kryo.register(ArrayList.class);
        kryo.register(HashSet.class);
        kryo.register(Line2D.Double.class, new Line2DSerializer());
        kryo.register(Rectangle.class);
        kryo.register(Area.class);
        kryo.register(Ellipse2D.Double.class);

        kryo.register(FindMatchRequest.class);
    }
}