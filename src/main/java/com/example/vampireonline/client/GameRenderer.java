package com.example.vampireonline.client;

import com.example.vampireonline.common.model.EnemyState;
import com.example.vampireonline.common.model.PlayerState;
import com.example.vampireonline.common.model.ProjectileState;
import com.example.vampireonline.common.model.WorldSnapshot;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

final public class GameRenderer {
    private static final Color[] PLAYER_COLORS = {
            Color.web("#4CC9F0"),
            Color.web("#F72585"),
            Color.web("#80ED99"),
            Color.web("#FFD166")
    };

    void render(Canvas canvas, WorldSnapshot snapshot, int localPlayerId) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double scale = Math.min(width / snapshot.width(), height / snapshot.height());
        double offsetX = (width - snapshot.width() * scale) * 0.5;
        double offsetY = (height - snapshot.height() * scale) * 0.5;

        g.setFill(Color.web("#11131A"));
        g.fillRect(0, 0, width, height);

        g.save();
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);

        drawArena(g, snapshot);
        for (ProjectileState projectile : snapshot.projectiles()) {
            drawProjectile(g, projectile);
        }
        for (EnemyState enemy : snapshot.enemies()) {
            drawEnemy(g, enemy);
        }
        for (PlayerState player : snapshot.players()) {
            drawPlayer(g, player, player.id() == localPlayerId);
        }

        g.restore();
        drawHud(g, snapshot, localPlayerId);
    }

    private void drawArena(GraphicsContext g, WorldSnapshot snapshot) {
        g.setFill(Color.web("#171A24"));
        g.fillRect(0, 0, snapshot.width(), snapshot.height());
        g.setStroke(Color.web("#282D3A"));
        g.setLineWidth(1);
        for (int x = 0; x <= snapshot.width(); x += 64) {
            g.strokeLine(x, 0, x, snapshot.height());
        }
        for (int y = 0; y <= snapshot.height(); y += 64) {
            g.strokeLine(0, y, snapshot.width(), y);
        }
    }

    private void drawPlayer(GraphicsContext g, PlayerState player, boolean local) {
        Color color = PLAYER_COLORS[Math.floorMod(player.colorIndex(), PLAYER_COLORS.length)];
        g.setFill(color);
        g.fillOval(player.x() - 18, player.y() - 18, 36, 36);
        g.setStroke(local ? Color.WHITE : Color.web("#0B0D12"));
        g.setLineWidth(local ? 4 : 2);
        g.strokeOval(player.x() - 18, player.y() - 18, 36, 36);

        g.setFill(Color.WHITE);
        g.setFont(Font.font("Consolas", 13));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(player.name(), player.x(), player.y() - 31);

        g.setFill(Color.web("#2B2E38"));
        g.fillRect(player.x() - 24, player.y() + 25, 48, 6);
        g.setFill(Color.web("#EF476F"));
        g.fillRect(player.x() - 24, player.y() + 25, 48 * Math.max(0, player.hp()) / 100.0, 6);
    }

    private void drawEnemy(GraphicsContext g, EnemyState enemy) {
        g.setFill(Color.web("#8A1C2D"));
        g.fillOval(enemy.x() - enemy.radius(), enemy.y() - enemy.radius(), enemy.radius() * 2, enemy.radius() * 2);
        g.setStroke(Color.web("#FF5C7A"));
        g.setLineWidth(2);
        g.strokeOval(enemy.x() - enemy.radius(), enemy.y() - enemy.radius(), enemy.radius() * 2, enemy.radius() * 2);
    }

    private void drawProjectile(GraphicsContext g, ProjectileState projectile) {
        g.setFill(Color.web("#FEE440"));
        g.fillOval(projectile.x() - 5, projectile.y() - 5, 10, 10);
    }

    private void drawHud(GraphicsContext g, WorldSnapshot snapshot, int localPlayerId) {
        g.setFont(Font.font("Consolas", 16));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        g.setFill(Color.WHITE);
        g.fillText("Players " + snapshot.players().size() + "/4  Enemies " + snapshot.enemies().size(), 16, 14);

        double y = 40;
        for (PlayerState player : snapshot.players()) {
            Color color = PLAYER_COLORS[Math.floorMod(player.colorIndex(), PLAYER_COLORS.length)];
            g.setFill(color);
            g.fillText((player.id() == localPlayerId ? "> " : "  ") + player.name()
                    + "  HP " + player.hp()
                    + "  Score " + player.score(), 16, y);
            y += 22;
        }
    }
}

