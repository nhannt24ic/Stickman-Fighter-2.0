package org.keyyh.stickmanfighter.client.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.keyyh.stickmanfighter.common.network.KryoManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class NetworkClient {
    private static NetworkClient instance;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private final int serverPort = 9876;
    private final Kryo kryo;
    private volatile boolean running = false;
    private final List<PacketListener> listeners = new ArrayList<>();

    private NetworkClient() {
        this.kryo = new Kryo();
        KryoManager.register(this.kryo);
        try {
            this.socket = new DatagramSocket();
            this.serverAddress = InetAddress.getByName("localhost");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    public void start() {
        if (running) return;
        running = true;
        Thread listenerThread = new Thread(this::listen);
        listenerThread.setDaemon(true);
        listenerThread.start();
        System.out.println("NetworkClient listener started.");
    }

    private void listen() {
        while (running) {
            try {
                byte[] buffer = new byte[4096];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                com.esotericsoftware.kryo.io.Input input = new com.esotericsoftware.kryo.io.Input(bais);
                Object receivedObject = kryo.readClassAndObject(input);
                input.close();

                // Thông báo cho tất cả các listener đã đăng ký
                synchronized (listeners) {
                    for (PacketListener listener : listeners) {
                        listener.received(receivedObject);
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    public void send(Serializable object) {
        if (socket == null || socket.isClosed()) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output output = new Output(baos);
            kryo.writeClassAndObject(output, object);
            output.close();
            byte[] buffer = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addListener(PacketListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(PacketListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    // Một interface để các màn hình có thể "lắng nghe" gói tin từ NetworkClient
    public interface PacketListener {
        void received(Object packet);
    }
}