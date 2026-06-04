package com.example.vampireonline.client;

import com.example.vampireonline.common.net.Protocol;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

final public class MenuController {
    @FXML
    private TextField nameField;
    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private Label statusLabel;

    private HostHandler hostHandler;
    private JoinHandler joinHandler;

    @FXML
    private void initialize() {
        nameField.setText("Player");
        hostField.setText("127.0.0.1");
        portField.setText(Integer.toString(Protocol.PORT));
        statusLabel.setText("혼자 하거나, 같이 플레이하세요.");
    }

    void configure(HostHandler hostHandler, JoinHandler joinHandler) {
        this.hostHandler = hostHandler;
        this.joinHandler = joinHandler;
    }

    @FXML
    private void hostLocal() {
        try {
            hostHandler.host(nameField.getText(), parsePort());
        } catch (Exception e) {
            statusLabel.setText("Host failed: " + e.getMessage());
        }
    }

    @FXML
    private void joinServer() {
        try {
            joinHandler.join(nameField.getText(), hostField.getText().strip(), parsePort());
        } catch (Exception e) {
            statusLabel.setText("Join failed: " + e.getMessage());
        }
    }

    private int parsePort() {
        return Integer.parseInt(portField.getText().strip());
    }

    @FunctionalInterface
    interface HostHandler {
        void host(String name, int port) throws Exception;
    }

    @FunctionalInterface
    interface JoinHandler {
        void join(String name, String host, int port) throws Exception;
    }
}

