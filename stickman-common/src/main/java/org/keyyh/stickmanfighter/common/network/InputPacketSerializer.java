package org.keyyh.stickmanfighter.common.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.keyyh.stickmanfighter.common.data.InputPacket;
import org.keyyh.stickmanfighter.common.enums.ActionModifier;
import org.keyyh.stickmanfighter.common.enums.PlayerAction;

import java.util.HashSet;

public class InputPacketSerializer extends Serializer<InputPacket> {
    @Override
    public void write(Kryo kryo, Output output, InputPacket packet) {
        // Ghi hành động chính bằng số thứ tự (ordinal) - rất nhỏ gọn
        output.writeInt(packet.primaryAction.ordinal());

        // Ghi số lượng các phím bổ trợ
        output.writeInt(packet.modifiers.size());
        // Ghi lần lượt ordinal của từng phím bổ trợ
        for (ActionModifier modifier : packet.modifiers) {
            output.writeInt(modifier.ordinal());
        }
    }

    @Override
    public InputPacket read(Kryo kryo, Input input, Class<? extends InputPacket> type) {
        InputPacket packet = new InputPacket();
        packet.modifiers = new HashSet<>();

        // Đọc hành động chính
        int primaryActionOrdinal = input.readInt();
        packet.primaryAction = PlayerAction.values()[primaryActionOrdinal];

        // Đọc số lượng phím bổ trợ
        int modifierCount = input.readInt();
        // Đọc lần lượt từng phím bổ trợ và thêm vào Set
        ActionModifier[] allModifiers = ActionModifier.values();
        for (int i = 0; i < modifierCount; i++) {
            int modifierOrdinal = input.readInt();
            packet.modifiers.add(allModifiers[modifierOrdinal]);
        }

        return packet;
    }
}