package com.sanbong.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/** Central place that owns the primary Stage and swaps its root scene. */
public final class SceneManager {

    private static Stage stage;

    private SceneManager() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void showLogin() throws IOException {
        setRoot("/fxml/login.fxml", 1040, 680);
    }

    public static void showRegister() throws IOException {
        setRoot("/fxml/register.fxml", 1040, 680);
    }

    public static void showShell() throws IOException {
        setRoot("/fxml/shell.fxml", 1200, 720);
    }

    private static void setRoot(String fxmlPath, double width, double height) throws IOException {
        URL url = SceneManager.class.getResource(fxmlPath);
        if (url == null) {
            throw new IOException("Không tìm thấy FXML: " + fxmlPath);
        }
        Parent root = FXMLLoader.load(url);
        Scene scene = new Scene(root, width, height);
        URL css = SceneManager.class.getResource("/css/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
    }

    public static Stage getStage() {
        return stage;
    }
}
