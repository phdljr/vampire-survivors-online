package com.example.vampireonline.server;

import com.example.vampireonline.common.net.InputFrame;

final class PlayerRuntime {
    final int id;
    final String name;
    final int colorIndex;
    double x;
    double y;
    int hp = 100;
    int score;
    double fireCooldown;
    InputFrame input = InputFrame.idle();

    PlayerRuntime(int id, String name, int colorIndex, double x, double y) {
        this.id = id;
        this.name = name;
        this.colorIndex = colorIndex;
        this.x = x;
        this.y = y;
    }
}

