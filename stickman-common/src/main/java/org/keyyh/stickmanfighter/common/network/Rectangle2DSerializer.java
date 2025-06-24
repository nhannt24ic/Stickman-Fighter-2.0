package org.keyyh.stickmanfighter.common.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.awt.geom.Rectangle2D;

public class Rectangle2DSerializer extends Serializer<Rectangle2D.Double> {

    @Override
    public void write(Kryo kryo, Output output, Rectangle2D.Double rectangle) {

        output.writeDouble(rectangle.x);
        output.writeDouble(rectangle.y);
        output.writeDouble(rectangle.width);
        output.writeDouble(rectangle.height);
    }

    @Override
    public Rectangle2D.Double read(Kryo kryo, Input input, Class<? extends Rectangle2D.Double> type) {

        double x = input.readDouble();
        double y = input.readDouble();
        double width = input.readDouble();
        double height = input.readDouble();

        return new Rectangle2D.Double(x, y, width, height);
    }
}