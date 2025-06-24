package org.keyyh.stickmanfighter.common.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.awt.geom.Line2D;

public class Line2DSerializer extends Serializer<Line2D.Double> {
    @Override
    public void write(Kryo kryo, Output output, Line2D.Double line2D) {

        output.writeDouble(line2D.x1);
        output.writeDouble(line2D.y1);
        output.writeDouble(line2D.x2);
        output.writeDouble(line2D.y2);
    }

    @Override
    public Line2D.Double read(Kryo kryo, Input input, Class<? extends Line2D.Double> type) {

        double x1 = input.readDouble();
        double y1 = input.readDouble();
        double x2 = input.readDouble();
        double y2 = input.readDouble();

        return new Line2D.Double(x1, y1, x2, y2);
    }
}
