package com.example.vampireonline.common.net;

import com.example.vampireonline.common.model.EnemyState;
import com.example.vampireonline.common.model.PlayerState;
import com.example.vampireonline.common.model.ProjectileState;
import com.example.vampireonline.common.model.WorldSnapshot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Protocol {
    public static final int PORT = 7777;
    public static final int MAX_PLAYERS = 4;

    public static final byte HELLO = 1;
    public static final byte WELCOME = 2;
    public static final byte INPUT = 3;
    public static final byte SNAPSHOT = 4;
    public static final byte REJECTED = 5;

    private Protocol() {
    }

    public static void writeHello(DataOutputStream out, String name) throws IOException {
        out.writeByte(HELLO);
        out.writeUTF(name == null || name.isBlank() ? "Player" : name.strip());
        out.flush();
    }

    public static String readHello(DataInputStream in) throws IOException {
        byte type = in.readByte();
        if (type != HELLO) {
            throw new IOException("Expected HELLO but got " + type);
        }
        return in.readUTF();
    }

    public static void writeWelcome(DataOutputStream out, int playerId) throws IOException {
        out.writeByte(WELCOME);
        out.writeInt(playerId);
        out.flush();
    }

    public static int readWelcome(DataInputStream in) throws IOException {
        byte type = in.readByte();
        if (type == REJECTED) {
            throw new IOException(in.readUTF());
        }
        if (type != WELCOME) {
            throw new IOException("Expected WELCOME but got " + type);
        }
        return in.readInt();
    }

    public static void writeRejected(DataOutputStream out, String reason) throws IOException {
        out.writeByte(REJECTED);
        out.writeUTF(reason);
        out.flush();
    }

    public static void writeInput(DataOutputStream out, InputFrame input) throws IOException {
        out.writeByte(INPUT);
        out.writeLong(input.sequence());
        out.writeBoolean(input.up());
        out.writeBoolean(input.down());
        out.writeBoolean(input.left());
        out.writeBoolean(input.right());
        out.writeBoolean(input.firing());
        out.writeDouble(input.aimX());
        out.writeDouble(input.aimY());
        out.flush();
    }

    public static InputFrame readInput(DataInputStream in) throws IOException {
        byte type = in.readByte();
        if (type != INPUT) {
            throw new IOException("Expected INPUT but got " + type);
        }
        return new InputFrame(
                in.readLong(),
                in.readBoolean(),
                in.readBoolean(),
                in.readBoolean(),
                in.readBoolean(),
                in.readBoolean(),
                in.readDouble(),
                in.readDouble()
        );
    }

    public static void writeSnapshot(DataOutputStream out, WorldSnapshot snapshot) throws IOException {
        out.writeByte(SNAPSHOT);
        out.writeLong(snapshot.tick());
        out.writeDouble(snapshot.width());
        out.writeDouble(snapshot.height());

        out.writeInt(snapshot.players().size());
        for (PlayerState player : snapshot.players()) {
            out.writeInt(player.id());
            out.writeUTF(player.name());
            out.writeDouble(player.x());
            out.writeDouble(player.y());
            out.writeInt(player.hp());
            out.writeInt(player.colorIndex());
            out.writeInt(player.score());
        }

        out.writeInt(snapshot.enemies().size());
        for (EnemyState enemy : snapshot.enemies()) {
            out.writeInt(enemy.id());
            out.writeDouble(enemy.x());
            out.writeDouble(enemy.y());
            out.writeDouble(enemy.radius());
            out.writeInt(enemy.hp());
        }

        out.writeInt(snapshot.projectiles().size());
        for (ProjectileState projectile : snapshot.projectiles()) {
            out.writeInt(projectile.id());
            out.writeInt(projectile.ownerId());
            out.writeDouble(projectile.x());
            out.writeDouble(projectile.y());
        }
        out.flush();
    }

    public static WorldSnapshot readSnapshot(DataInputStream in) throws IOException {
        byte type = in.readByte();
        if (type != SNAPSHOT) {
            throw new IOException("Expected SNAPSHOT but got " + type);
        }

        long tick = in.readLong();
        double width = in.readDouble();
        double height = in.readDouble();

        int playerCount = in.readInt();
        List<PlayerState> players = new ArrayList<>(playerCount);
        for (int i = 0; i < playerCount; i++) {
            players.add(new PlayerState(
                    in.readInt(),
                    in.readUTF(),
                    in.readDouble(),
                    in.readDouble(),
                    in.readInt(),
                    in.readInt(),
                    in.readInt()
            ));
        }

        int enemyCount = in.readInt();
        List<EnemyState> enemies = new ArrayList<>(enemyCount);
        for (int i = 0; i < enemyCount; i++) {
            enemies.add(new EnemyState(in.readInt(), in.readDouble(), in.readDouble(), in.readDouble(), in.readInt()));
        }

        int projectileCount = in.readInt();
        List<ProjectileState> projectiles = new ArrayList<>(projectileCount);
        for (int i = 0; i < projectileCount; i++) {
            projectiles.add(new ProjectileState(in.readInt(), in.readInt(), in.readDouble(), in.readDouble()));
        }

        return new WorldSnapshot(tick, width, height, List.copyOf(players), List.copyOf(enemies), List.copyOf(projectiles));
    }
}

