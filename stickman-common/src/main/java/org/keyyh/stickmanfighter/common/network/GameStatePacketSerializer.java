package org.keyyh.stickmanfighter.common.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.keyyh.stickmanfighter.common.data.CharacterState;
import org.keyyh.stickmanfighter.common.data.GameStatePacket;

import java.util.ArrayList;

public class GameStatePacketSerializer extends Serializer<GameStatePacket> {
    @Override
    public void write(Kryo kryo, Output output, GameStatePacket packet) {
        output.writeLong(packet.timestamp);

        // Ghi số lượng người chơi để biết cần đọc bao nhiêu lần
        output.writeInt(packet.players.size());

        // Dùng kryo để ghi từng đối tượng CharacterState một
        for (CharacterState state : packet.players) {
            kryo.writeObject(output, state);
        }
    }

    @Override
    public GameStatePacket read(Kryo kryo, Input input, Class<? extends GameStatePacket> type) {
        GameStatePacket packet = new GameStatePacket();
        packet.timestamp = input.readLong();

        int playerCount = input.readInt();
        packet.players = new ArrayList<>(playerCount);

        for (int i = 0; i < playerCount; i++) {
            // Dùng kryo để đọc từng đối tượng CharacterState
            packet.players.add(kryo.readObject(input, CharacterState.class));
        }

        return packet;
    }
}