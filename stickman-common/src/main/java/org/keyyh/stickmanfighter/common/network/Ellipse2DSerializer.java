package org.keyyh.stickmanfighter.common.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.awt.geom.Ellipse2D;

public class Ellipse2DSerializer extends Serializer<Ellipse2D.Double> {

    @Override
    public void write(Kryo kryo, Output output, Ellipse2D.Double ellipse) {

        output.writeDouble(ellipse.x);
        output.writeDouble(ellipse.y);
        output.writeDouble(ellipse.width);
        output.writeDouble(ellipse.height);
    }

    @Override
    public Ellipse2D.Double read(Kryo kryo, Input input, Class<? extends Ellipse2D.Double> type) {

        double x = input.readDouble();
        double y = input.readDouble();
        double width = input.readDouble();
        double height = input.readDouble();

        return new Ellipse2D.Double(x, y, width, height);
    }
}