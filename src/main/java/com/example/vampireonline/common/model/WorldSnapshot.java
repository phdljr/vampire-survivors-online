package com.example.vampireonline.common.model;

import java.util.List;

public record WorldSnapshot(
        long tick,
        double width,
        double height,
        List<PlayerState> players,
        List<EnemyState> enemies,
        List<ProjectileState> projectiles
) {
    public static WorldSnapshot empty(double width, double height) {
        return new WorldSnapshot(0, width, height, List.of(), List.of(), List.of());
    }
}

