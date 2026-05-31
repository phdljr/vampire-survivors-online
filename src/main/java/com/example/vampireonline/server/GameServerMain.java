package com.example.vampireonline.server;

import com.example.vampireonline.common.net.Protocol;

public final class GameServerMain {
    private GameServerMain() {
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.PORT;
        try (GameServer server = new GameServer(port)) {
            System.out.println("Vampire Online server listening on port " + server.port());
            server.runBlocking();
        }
    }
}

