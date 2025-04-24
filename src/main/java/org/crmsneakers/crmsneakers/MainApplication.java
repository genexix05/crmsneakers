package org.crmsneakers.crmsneakers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.crmsneakers.crmsneakers.db.DatabaseService;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("CRM Sneakers - Login");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // Close database connection when application stops
        DatabaseService.getInstance().close();
    }

    public static void main(String[] args) {
        launch();
    }
}