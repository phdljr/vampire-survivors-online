package com.example.vampireonline.server;

import com.example.vampireonline.common.math.Vec2;
import com.example.vampireonline.common.model.EnemyState;
import com.example.vampireonline.common.model.PlayerState;
import com.example.vampireonline.common.model.ProjectileState;
import com.example.vampireonline.common.model.WorldSnapshot;
import com.example.vampireonline.common.net.InputFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;

final class ServerGame {
    static final double WIDTH = 1280;
    static final double HEIGHT = 720;

    private static final double PLAYER_RADIUS = 18;
    private static final double ENEMY_RADIUS = 16;
    private static final double PLAYER_SPEED = 260;
    private static final double PROJECTILE_SPEED = 620;
    private static final double PROJECTILE_RADIUS = 6;
    private static final int MAX_ENEMIES = 80;

    private final Map<Integer, PlayerRuntime> players = new HashMap<>();
    private final List<EnemyRuntime> enemies = new ArrayList<>();
    private final List<ProjectileRuntime> projectiles = new ArrayList<>();
    private final Random random = new Random();
    private long tick;
    private int nextPlayerId = 1;
    private int nextEnemyId = 1;
    private int nextProjectileId = 1;
    private double enemySpawnTimer;

    synchronized OptionalInt addPlayer(String requestedName) {
        if (players.size() >= 4) {
            return OptionalInt.empty();
        }
        int id = nextPlayerId++;
        String name = sanitizeName(requestedName, id);
        double angle = players.size() * Math.PI * 0.5;
        double x = WIDTH * 0.5 + Math.cos(angle) * 90;
        double y = HEIGHT * 0.5 + Math.sin(angle) * 90;
        players.put(id, new PlayerRuntime(id, name, id % 4, x, y));
        return OptionalInt.of(id);
    }

    synchronized void removePlayer(int id) {
        players.remove(id);
    }

    synchronized void updateInput(int playerId, InputFrame input) {
        PlayerRuntime player = players.get(playerId);
        if (player != null && input.sequence() >= player.input.sequence()) {
            player.input = input;
        }
    }

    synchronized void update(double dt) {
        tick++;
        updatePlayers(dt);
        updateProjectiles(dt);
        updateEnemies(dt);
        spawnEnemies(dt);
    }

    synchronized WorldSnapshot snapshot() {
        List<PlayerState> playerStates = players.values().stream()
                .map(p -> new PlayerState(p.id, p.name, p.x, p.y, p.hp, p.colorIndex, p.score))
                .toList();
        List<EnemyState> enemyStates = enemies.stream()
                .map(e -> new EnemyState(e.id, e.x, e.y, ENEMY_RADIUS, e.hp))
                .toList();
        List<ProjectileState> projectileStates = projectiles.stream()
                .map(p -> new ProjectileState(p.id, p.ownerId, p.x, p.y))
                .toList();
        return new WorldSnapshot(tick, WIDTH, HEIGHT, playerStates, enemyStates, projectileStates);
    }

    private void updatePlayers(double dt) {
        for (PlayerRuntime player : players.values()) {
            InputFrame input = player.input;
            double dx = (input.right() ? 1 : 0) - (input.left() ? 1 : 0);
            double dy = (input.down() ? 1 : 0) - (input.up() ? 1 : 0);
            Vec2 movement = new Vec2(dx, dy).normalizeOrZero().scale(PLAYER_SPEED * dt);
            Vec2 position = new Vec2(player.x, player.y)
                    .add(movement)
                    .clamp(PLAYER_RADIUS, PLAYER_RADIUS, WIDTH - PLAYER_RADIUS, HEIGHT - PLAYER_RADIUS);
            player.x = position.x();
            player.y = position.y();

            player.fireCooldown = Math.max(0, player.fireCooldown - dt);
            if (input.firing() && player.fireCooldown <= 0) {
                fireProjectile(player);
                player.fireCooldown = 0.16;
            }
        }
    }

    private void fireProjectile(PlayerRuntime player) {
        Vec2 origin = new Vec2(player.x, player.y);
        Vec2 direction = new Vec2(player.input.aimX(), player.input.aimY()).subtract(origin).normalizeOrZero();
        if (direction == Vec2.ZERO) {
            direction = new Vec2(1, 0);
        }
        projectiles.add(new ProjectileRuntime(nextProjectileId++, player.id, player.x, player.y,
                direction.x() * PROJECTILE_SPEED, direction.y() * PROJECTILE_SPEED));
    }

    private void updateProjectiles(double dt) {
        Iterator<ProjectileRuntime> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            ProjectileRuntime projectile = iterator.next();
            projectile.x += projectile.vx * dt;
            projectile.y += projectile.vy * dt;
            projectile.life -= dt;

            boolean removed = projectile.life <= 0
                    || projectile.x < -40
                    || projectile.y < -40
                    || projectile.x > WIDTH + 40
                    || projectile.y > HEIGHT + 40;
            if (removed) {
                iterator.remove();
                continue;
            }

            for (EnemyRuntime enemy : enemies) {
                if (distanceSquared(projectile.x, projectile.y, enemy.x, enemy.y) <= square(ENEMY_RADIUS + PROJECTILE_RADIUS)) {
                    enemy.hp -= 25;
                    PlayerRuntime owner = players.get(projectile.ownerId);
                    if (owner != null && enemy.hp <= 0) {
                        owner.score += 10;
                    }
                    iterator.remove();
                    break;
                }
            }
        }
        enemies.removeIf(enemy -> enemy.hp <= 0);
    }

    private void updateEnemies(double dt) {
        for (EnemyRuntime enemy : enemies) {
            PlayerRuntime target = nearestPlayer(enemy.x, enemy.y);
            if (target == null) {
                continue;
            }
            Vec2 direction = new Vec2(target.x - enemy.x, target.y - enemy.y).normalizeOrZero();
            enemy.x += direction.x() * enemy.speed * dt;
            enemy.y += direction.y() * enemy.speed * dt;

            if (distanceSquared(enemy.x, enemy.y, target.x, target.y) <= square(ENEMY_RADIUS + PLAYER_RADIUS)) {
                target.hp = Math.max(0, target.hp - 1);
            }
        }
    }

    private void spawnEnemies(double dt) {
        enemySpawnTimer -= dt;
        if (enemySpawnTimer > 0 || enemies.size() >= MAX_ENEMIES || players.isEmpty()) {
            return;
        }
        enemySpawnTimer = Math.max(0.12, 0.75 - tick / 5_000.0);

        double side = random.nextInt(4);
        double x = switch ((int) side) {
            case 0 -> -20;
            case 1 -> WIDTH + 20;
            default -> random.nextDouble(WIDTH);
        };
        double y = switch ((int) side) {
            case 2 -> -20;
            case 3 -> HEIGHT + 20;
            default -> random.nextDouble(HEIGHT);
        };
        enemies.add(new EnemyRuntime(nextEnemyId++, x, y, 55 + random.nextDouble(45), 50));
    }

    private PlayerRuntime nearestPlayer(double x, double y) {
        PlayerRuntime nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (PlayerRuntime player : players.values()) {
            if (player.hp <= 0) {
                continue;
            }
            double distance = distanceSquared(x, y, player.x, player.y);
            if (distance < bestDistance) {
                nearest = player;
                bestDistance = distance;
            }
        }
        return nearest;
    }

    private static String sanitizeName(String name, int id) {
        if (name == null || name.isBlank()) {
            return "Player " + id;
        }
        String stripped = name.strip();
        return stripped.length() <= 16 ? stripped : stripped.substring(0, 16);
    }

    private static double distanceSquared(double ax, double ay, double bx, double by) {
        return square(ax - bx) + square(ay - by);
    }

    private static double square(double value) {
        return value * value;
    }

    private static final class EnemyRuntime {
        final int id;
        final double speed;
        double x;
        double y;
        int hp;

        EnemyRuntime(int id, double x, double y, double speed, int hp) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.hp = hp;
        }
    }

    private static final class ProjectileRuntime {
        final int id;
        final int ownerId;
        final double vx;
        final double vy;
        double x;
        double y;
        double life = 1.5;

        ProjectileRuntime(int id, int ownerId, double x, double y, double vx, double vy) {
            this.id = id;
            this.ownerId = ownerId;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }
}

