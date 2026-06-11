package com.example.vampireonline.server;

import com.example.vampireonline.common.model.WorldSnapshot;
import com.example.vampireonline.common.net.Protocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GameServer implements AutoCloseable {
    private static final double TICK_SECONDS = 1.0 / 30.0;

    private final ServerGame game = new ServerGame();
    private final ConcurrentMap<Integer, ClientConnection> clients = new ConcurrentHashMap<>();
    private final ServerSocket serverSocket;
    private volatile boolean running = true;

    public GameServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public void startAsync() {
        Thread acceptThread = Thread.ofPlatform().name("server-accept").daemon(true).start(this::acceptLoop);
        Thread tickThread = Thread.ofPlatform().name("server-tick").daemon(true).start(this::tickLoop);
        if (!acceptThread.isAlive() || !tickThread.isAlive()) {
            throw new IllegalStateException("Failed to start server threads");
        }
    }

    public void runBlocking() {
        startAsync();
        while (running) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                close();
            }
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // Closing during shutdown is best-effort.
        }
        for (ClientConnection client : clients.values()) {
            client.close();
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                configure(socket);
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                String name = Protocol.readHello(in);

                OptionalInt playerId = game.addPlayer(name);
                if (playerId.isEmpty()) {
                    Protocol.writeRejected(out, "Server is full. Maximum players: " + Protocol.MAX_PLAYERS);
                    socket.close();
                    continue;
                }

                int id = playerId.getAsInt();
                Protocol.writeWelcome(out, id);
                ClientConnection connection = new ClientConnection(id, socket, in, out);
                clients.put(id, connection);
                Thread.ofPlatform().name("client-" + id + "-input").daemon(true).start(() -> readInputs(connection));
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void readInputs(ClientConnection connection) {
        try {
            while (running && !connection.closed) {
                byte type = Protocol.readClientMessageType(connection.in);
                if (type == Protocol.INPUT) {
                    game.updateInput(connection.playerId, Protocol.readInputPayload(connection.in));
                } else if (type == Protocol.UPGRADE_CHOICE) {
                    game.chooseUpgrade(connection.playerId, Protocol.readUpgradeChoicePayload(connection.in));
                } else {
                    throw new IOException("Unsupported client message: " + type);
                }
            }
        } catch (IOException ignored) {
            disconnect(connection.playerId);
        }
    }

    private void tickLoop() {
        long next = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            if (now < next) {
                sleepNanos(next - now);
                continue;
            }

            game.update(TICK_SECONDS);
            broadcast(game.snapshot());
            next += (long) (TICK_SECONDS * 1_000_000_000L);
            if (System.nanoTime() - next > 250_000_000L) {
                next = System.nanoTime();
            }
        }
    }

    private void broadcast(WorldSnapshot snapshot) {
        List<Integer> failed = new ArrayList<>();
        for (ClientConnection client : clients.values()) {
            try {
                synchronized (client.out) {
                    Protocol.writeSnapshot(client.out, snapshot);
                }
            } catch (IOException e) {
                failed.add(client.playerId);
            }
        }
        failed.forEach(this::disconnect);
    }

    private void disconnect(int playerId) {
        ClientConnection removed = clients.remove(playerId);
        game.removePlayer(playerId);
        if (removed != null) {
            removed.close();
        }
    }

    private static void configure(Socket socket) throws SocketException {
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
    }

    private static void sleepNanos(long nanos) {
        try {
            long millis = nanos / 1_000_000L;
            int extraNanos = (int) (nanos % 1_000_000L);
            Thread.sleep(millis, extraNanos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class ClientConnection implements AutoCloseable {
        final int playerId;
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;
        volatile boolean closed;

        ClientConnection(int playerId, Socket socket, DataInputStream in, DataOutputStream out) {
            this.playerId = playerId;
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        @Override
        public void close() {
            closed = true;
            try {
                socket.close();
            } catch (IOException ignored) {
                // Closing during disconnect is best-effort.
            }
        }
    }
}
