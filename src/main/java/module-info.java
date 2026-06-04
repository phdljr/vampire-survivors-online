module com.example.vampireonline {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;

    exports com.example.vampireonline.client;
    exports com.example.vampireonline.server;

    opens com.example.vampireonline.client to javafx.fxml;
}
