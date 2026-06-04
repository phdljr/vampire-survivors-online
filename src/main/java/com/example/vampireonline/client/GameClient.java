package com.example.vampireonline.client;

import com.example.vampireonline.common.model.WorldSnapshot;
import com.example.vampireonline.common.net.InputFrame;
import com.example.vampireonline.common.net.Protocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

final public class GameClient implements AutoCloseable {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final AtomicReference<WorldSnapshot> latestSnapshot =
            new AtomicReference<>(WorldSnapshot.empty(1280, 720));
    private volatile boolean running = true;
    private int playerId;

    GameClient(String host, int port, String name) throws IOException {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        Protocol.writeHello(out, name);
        playerId = Protocol.readWelcome(in);
        Thread.ofPlatform().name("snapshot-reader").daemon(true).start(this::readSnapshots);
    }

    int playerId() {
        return playerId;
    }

    WorldSnapshot latestSnapshot() {
        return latestSnapshot.get();
    }

    void sendInput(InputFrame input) {
        if (!running) {
            return;
        }
        try {
            synchronized (out) {
                Protocol.writeInput(out, input);
            }
        } catch (IOException e) {
            close();
        }
    }

    void sendUpgradeChoice(String type) {
        if (!running) {
            return;
        }
        try {
            synchronized (out) {
                Protocol.writeUpgradeChoice(out, type);
            }
        } catch (IOException e) {
            close();
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {
            // Closing during shutdown is best-effort.
        }
    }

    private void readSnapshots() {
        try {
            while (running) {
                latestSnapshot.set(Protocol.readSnapshot(in));
            }
        } catch (IOException ignored) {
            close();
        }
    }
}
