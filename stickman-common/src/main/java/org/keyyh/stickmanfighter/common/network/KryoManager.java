package org.keyyh.stickmanfighter.common.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import org.keyyh.stickmanfighter.common.data.*;
import org.keyyh.stickmanfighter.common.enums.*;
import org.keyyh.stickmanfighter.common.network.requests.*;
import org.keyyh.stickmanfighter.common.network.responses.*;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

public class KryoManager {

    public static void register(Kryo kryo) {
        // <<< THAY ĐỔI: Cấu hình serializer cho ArrayList một cách tường minh
        // Điều này sẽ sửa lỗi NullPointerException khi Kryo xử lý các danh sách Generic
        CollectionSerializer arrayListSerializer = new CollectionSerializer();
        arrayListSerializer.setElementsCanBeNull(false); // Giả định các phần tử trong list không bao giờ null
        kryo.register(ArrayList.class, arrayListSerializer);

        // Các lớp cũ
        kryo.register(InputPacket.class, new InputPacketSerializer());
        kryo.register(CharacterState.class);
        kryo.register(Pose.class);
        kryo.register(UUID.class, new UUIDSerializer());
        kryo.register(ConnectionResponsePacket.class);
//        kryo.register(GameStatePacket.class);
        kryo.register(HashSet.class);

        // Enums
        kryo.register(PlayerAction.class);
        kryo.register(ActionModifier.class);
        kryo.register(CharacterFSMState.class);

        // Lớp hình học
        kryo.register(Line2D.Double.class, new Line2DSerializer());
        kryo.register(Rectangle.class);
        kryo.register(Area.class);
        kryo.register(Ellipse2D.Double.class);

        // Các gói tin Lobby
        kryo.register(ListRoomsRequest.class);
        kryo.register(CreateRoomRequest.class);
        kryo.register(JoinRoomRequest.class);
        kryo.register(RoomInfo.class);
        kryo.register(RoomListResponse.class);
        kryo.register(GameStartPacket.class);
    }
}