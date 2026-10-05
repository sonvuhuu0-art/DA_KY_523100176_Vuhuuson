package com.sanbong;

import com.sanbong.db.DatabaseInitializer;
import com.sanbong.db.DatabaseSeeder;
import com.sanbong.util.SceneManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void init() {
        DatabaseInitializer.initialize();
        DatabaseSeeder.seedIfEmpty();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        SceneManager.init(primaryStage);
        primaryStage.setTitle("Quản Lý & Đặt Lịch Sân Bóng");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        SceneManager.showLogin();
        primaryStage.show();
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Đã xảy ra lỗi không mong muốn: " + throwable.getMessage());
                alert.setHeaderText("Lỗi hệ thống");
                alert.showAndWait();
            });
        });
        launch(args);
    }
}
