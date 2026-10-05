package com.sanbong.controller;

import com.sanbong.model.User;
import com.sanbong.service.AuthService;
import com.sanbong.util.FeatureRowFactory;
import com.sanbong.util.SceneManager;
import com.sanbong.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.Optional;

public class LoginController {

    @FXML
    private HBox feature1;
    @FXML
    private HBox feature2;
    @FXML
    private HBox feature3;
    @FXML
    private HBox feature4;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        FeatureRowFactory.fill(feature1, "field", "Đặt sân theo thời gian thực",
                "Xem khung giờ trống của từng sân, chọn ngày và đặt chỉ trong vài giây.");
        FeatureRowFactory.fill(feature2, "building", "Chủ sân tự quản lý",
                "Chủ sân toàn quyền quản lý cụm sân, giá theo khung giờ và lịch đặt của riêng mình.");
        FeatureRowFactory.fill(feature3, "chart", "Giám sát toàn hệ thống",
                "Quản trị viên theo dõi doanh thu, tỷ lệ lấp đầy và phê duyệt đối tác chủ sân.");
        FeatureRowFactory.fill(feature4, "booking", "Chống trùng lịch tuyệt đối",
                "Giao dịch đặt sân được xử lý an toàn, không xảy ra hai người đặt trùng một khung giờ.");
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ email và mật khẩu.");
            return;
        }

        try {
            Optional<User> user = authService.login(email, password);
            if (user.isEmpty()) {
                showError("Email hoặc mật khẩu không đúng.");
                return;
            }
            SessionManager.setCurrentUser(user.get());
            SceneManager.showShell();
        } catch (IllegalStateException e) {
            showError(e.getMessage());
        } catch (IOException e) {
            showError("Không thể mở màn hình chính: " + e.getMessage());
        }
    }

    @FXML
    private void goToRegister() {
        try {
            SceneManager.showRegister();
        } catch (IOException e) {
            showError("Không thể mở màn hình đăng ký.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }
}
