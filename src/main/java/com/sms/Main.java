package com.sms;

import com.sms.util.DatabaseInitializer;
import com.sms.view.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static final int WIDTH = 1360;
    public static final int HEIGHT = 820;

    @Override
    public void start(Stage stage) {
        DatabaseInitializer.initialize();

        LoginView loginView = new LoginView(stage);
        Scene scene = new Scene(loginView.getRoot(), WIDTH, HEIGHT);
        var stylesheet = Main.class.getResource("/css/style.css");
        if (stylesheet != null) scene.getStylesheets().add(stylesheet.toExternalForm());

        stage.setTitle("Smart Student Management System");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
