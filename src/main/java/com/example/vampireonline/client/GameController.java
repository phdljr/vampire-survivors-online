package com.example.vampireonline.client;

import com.example.vampireonline.common.model.PlayerState;
import com.example.vampireonline.common.model.WorldSnapshot;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;

final public class GameController {
    @FXML
    private StackPane rootPane;
    @FXML
    private Canvas gameCanvas;

    private final InputState input = new InputState();
    private final GameRenderer renderer = new GameRenderer();
    private GameClient client;
    private AnimationTimer timer;
    private GameOverHandler gameOverHandler;
    private boolean gameOver;

    @FXML
    private void initialize() {
        gameCanvas.widthProperty().bind(rootPane.widthProperty());
        gameCanvas.heightProperty().bind(rootPane.heightProperty());
    }

    void start(GameClient client, GameOverHandler gameOverHandler) {
        this.client = client;
        this.gameOverHandler = gameOverHandler;
        this.gameOver = false;
        timer = new AnimationTimer() {
            private long lastInputSent;

            @Override
            public void handle(long now) {
                WorldSnapshot snapshot = client.latestSnapshot();
                PlayerState localPlayer = findLocalPlayer(snapshot);
                if (!gameOver && localPlayer != null && localPlayer.hp() <= 0) {
                    gameOver = true;
                    gameOverHandler.onGameOver(localPlayer);
                    return;
                }

                double scale = Math.min(gameCanvas.getWidth() / snapshot.width(), gameCanvas.getHeight() / snapshot.height());
                double offsetX = Math.max(0, (gameCanvas.getWidth() - snapshot.width() * scale) * 0.5);
                double offsetY = Math.max(0, (gameCanvas.getHeight() - snapshot.height() * scale) * 0.5);
                if (now - lastInputSent >= 16_000_000L) {
                    client.sendInput(input.nextFrame(-offsetX / scale, -offsetY / scale, scale));
                    lastInputSent = now;
                }
                renderer.render(gameCanvas, snapshot, client.playerId());
            }
        };
        timer.start();
        Platform.runLater(rootPane::requestFocus);
    }

    void attachScene(Scene scene) {
        scene.setOnKeyPressed(event -> setKey(event.getCode(), true));
        scene.setOnKeyReleased(event -> setKey(event.getCode(), false));
        scene.setOnMouseMoved(event -> input.setAim(event.getX(), event.getY()));
        scene.setOnMouseDragged(event -> input.setAim(event.getX(), event.getY()));
        scene.setOnMousePressed(event -> input.setFiring(true));
        scene.setOnMouseReleased(event -> input.setFiring(false));
    }

    void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void setKey(KeyCode code, boolean pressed) {
        switch (code) {
            case W, UP -> input.setUp(pressed);
            case S, DOWN -> input.setDown(pressed);
            case A, LEFT -> input.setLeft(pressed);
            case D, RIGHT -> input.setRight(pressed);
            case SPACE -> input.setFiring(pressed);
            default -> {
            }
        }
    }

    private PlayerState findLocalPlayer(WorldSnapshot snapshot) {
        return snapshot.players().stream()
                .filter(player -> player.id() == client.playerId())
                .findFirst()
                .orElse(null);
    }

    @FunctionalInterface
    interface GameOverHandler {
        void onGameOver(PlayerState player);
    }
}
