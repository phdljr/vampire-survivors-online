package com.example.vampireonline.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public final class GameOverController {
    @FXML
    private Label scoreLabel;

    private Runnable backToStartHandler;
    private Runnable exitHandler;

    void configure(int score, Runnable backToStartHandler, Runnable exitHandler) {
        scoreLabel.setText(Integer.toString(score));
        this.backToStartHandler = backToStartHandler;
        this.exitHandler = exitHandler;
    }

    @FXML
    private void backToStart() {
        backToStartHandler.run();
    }

    @FXML
    private void exitGame() {
        exitHandler.run();
    }
}
