package com.example.vampireonline.client;

import com.example.vampireonline.common.model.WorldSnapshot;
import com.example.vampireonline.common.net.Protocol;
import com.example.vampireonline.server.GameServer;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class GameClientApp extends Application {
    private final InputState input = new InputState();
    private final GameRenderer renderer = new GameRenderer();
    private GameClient client;
    private GameServer embeddedServer;
    private AnimationTimer timer;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Vampire Online JavaFX");
        stage.setScene(menuScene(stage));
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        stage.show();
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
        if (client != null) {
            client.close();
        }
        if (embeddedServer != null) {
            embeddedServer.close();
        }
    }

    private Scene menuScene(Stage stage) {
        TextField nameField = new TextField("Player");
        TextField hostField = new TextField("127.0.0.1");
        TextField portField = new TextField(Integer.toString(Protocol.PORT));
        Label status = new Label("Host locally or join a server.");

        Button hostButton = new Button("Host Local");
        hostButton.setMaxWidth(Double.MAX_VALUE);
        hostButton.setOnAction(event -> {
            try {
                if (embeddedServer == null) {
                    embeddedServer = new GameServer(Integer.parseInt(portField.getText().strip()));
                    embeddedServer.startAsync();
                }
                connect(stage, "127.0.0.1", embeddedServer.port(), nameField.getText());
            } catch (Exception e) {
                status.setText("Host failed: " + e.getMessage());
            }
        });

        Button joinButton = new Button("Join");
        joinButton.setMaxWidth(Double.MAX_VALUE);
        joinButton.setOnAction(event -> {
            try {
                connect(stage, hostField.getText().strip(), Integer.parseInt(portField.getText().strip()), nameField.getText());
            } catch (Exception e) {
                status.setText("Join failed: " + e.getMessage());
            }
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(24));
        form.addRow(0, new Label("Name"), nameField);
        form.addRow(1, new Label("Host"), hostField);
        form.addRow(2, new Label("Port"), portField);
        HBox buttons = new HBox(10, hostButton, joinButton);
        HBox.setHgrow(hostButton, Priority.ALWAYS);
        HBox.setHgrow(joinButton, Priority.ALWAYS);
        form.add(buttons, 1, 3);
        form.add(status, 1, 4);

        BorderPane root = new BorderPane(form);
        root.setStyle("-fx-background-color: #11131A; -fx-text-fill: white;");
        form.setStyle("-fx-background-color: #F4F5F7;");
        return new Scene(root, 960, 600);
    }

    private void connect(Stage stage, String host, int port, String name) throws Exception {
        client = new GameClient(host, port, name);
        stage.setScene(gameScene());
    }

    private Scene gameScene() {
        Canvas canvas = new Canvas(1280, 720);
        StackPane root = new StackPane(canvas);
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        Scene scene = new Scene(root, 1280, 720);
        scene.setOnKeyPressed(event -> setKey(event.getCode(), true));
        scene.setOnKeyReleased(event -> setKey(event.getCode(), false));
        scene.setOnMouseMoved(event -> input.setAim(event.getX(), event.getY()));
        scene.setOnMouseDragged(event -> input.setAim(event.getX(), event.getY()));
        scene.setOnMousePressed(event -> input.setFiring(true));
        scene.setOnMouseReleased(event -> input.setFiring(false));

        timer = new AnimationTimer() {
            private long lastInputSent;

            @Override
            public void handle(long now) {
                WorldSnapshot snapshot = client.latestSnapshot();
                double scale = Math.min(canvas.getWidth() / snapshot.width(), canvas.getHeight() / snapshot.height());
                double offsetX = Math.max(0, (canvas.getWidth() - snapshot.width() * scale) * 0.5);
                double offsetY = Math.max(0, (canvas.getHeight() - snapshot.height() * scale) * 0.5);
                if (now - lastInputSent >= 16_000_000L) {
                    client.sendInput(input.nextFrame(-offsetX / scale, -offsetY / scale, scale));
                    lastInputSent = now;
                }
                renderer.render(canvas, snapshot, client.playerId());
            }
        };
        timer.start();

        Platform.runLater(root::requestFocus);
        return scene;
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

    public static void main(String[] args) {
        launch(args);
    }
}

