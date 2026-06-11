package com.example.vampireonline.client;

import com.example.vampireonline.common.model.PlayerState;
import com.example.vampireonline.common.model.UpgradeCard;
import com.example.vampireonline.common.model.WorldSnapshot;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

final public class GameController {
    @FXML
    private StackPane rootPane;
    @FXML
    private Canvas gameCanvas;
    @FXML
    private VBox upgradePane;
    @FXML
    private HBox upgradeCardBox;
    @FXML
    private Label upgradeTitleLabel;

    private final InputState input = new InputState();
    private final GameRenderer renderer = new GameRenderer();
    private GameClient client;
    private AnimationTimer timer;
    private boolean gameOver;
    private List<UpgradeCard> visibleCards = List.of();

    @FXML
    private void initialize() {
        gameCanvas.widthProperty().bind(rootPane.widthProperty());
        gameCanvas.heightProperty().bind(rootPane.heightProperty());
    }

    void start(GameClient client, GameOverHandler gameOverHandler) {
        this.client = client;
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
                updateUpgradePane(localPlayer);

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

    private void updateUpgradePane(PlayerState localPlayer) {
        List<UpgradeCard> cards = localPlayer == null ? List.of() : localPlayer.pendingUpgrades();
        if (cards.equals(visibleCards)) {
            return;
        }
        visibleCards = cards;
        upgradeCardBox.getChildren().clear();
        boolean hasCards = !cards.isEmpty();
        upgradePane.setVisible(hasCards);
        upgradePane.setManaged(hasCards);
        if (!hasCards) {
            return;
        }

        upgradeTitleLabel.setText("Level " + localPlayer.level() + " Upgrade");
        for (UpgradeCard card : cards) {
            Button button = new Button(card.title() + "\nLv." + card.nextLevel() + "\n" + card.description());
            button.getStyleClass().add("upgrade-card");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setPrefWidth(230);
            button.setPrefHeight(132);
            button.setWrapText(true);
            button.setOnAction(event -> {
                client.sendUpgradeChoice(card.type());
                upgradePane.setVisible(false);
                upgradePane.setManaged(false);
                visibleCards = List.of();
            });
            upgradeCardBox.getChildren().add(button);
        }
    }

    @FunctionalInterface
    interface GameOverHandler {
        void onGameOver(PlayerState player);
    }
}
