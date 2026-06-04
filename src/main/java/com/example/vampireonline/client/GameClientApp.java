package com.example.vampireonline.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.example.vampireonline.server.GameServer;

import java.io.IOException;

public final class GameClientApp extends Application {
    private GameClient client;
    private GameServer embeddedServer;
    private GameController gameController;

    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle("Vampire Online JavaFX");
        stage.setScene(loadMenuScene(stage));
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        stage.show();
    }

    @Override
    public void stop() {
        if (gameController != null) {
            gameController.stop();
        }
        if (client != null) {
            client.close();
        }
        if (embeddedServer != null) {
            embeddedServer.close();
        }
    }

    private Scene loadMenuScene(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(GameClientApp.class.getResource("menu.fxml"));
        Parent root = loader.load();
        MenuController controller = loader.getController();
        controller.configure(
                (name, port) -> {
                    if (embeddedServer == null) {
                        embeddedServer = new GameServer(port);
                        embeddedServer.startAsync();
                    }
                    connect(stage, "127.0.0.1", embeddedServer.port(), name);
                },
                (name, host, port) -> connect(stage, host, port, name)
        );
        return new Scene(root, 960, 600);
    }

    private void connect(Stage stage, String host, int port, String name) throws IOException {
        client = new GameClient(host, port, name);
        FXMLLoader loader = new FXMLLoader(GameClientApp.class.getResource("game.fxml"));
        Parent root = loader.load();
        gameController = loader.getController();
        gameController.start(client);

        Scene scene = new Scene(root, 1280, 720);
        gameController.attachScene(scene);
        stage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
