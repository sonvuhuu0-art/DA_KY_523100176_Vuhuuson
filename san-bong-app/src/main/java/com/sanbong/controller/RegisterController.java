package com.sanbong.controller;

import com.sanbong.service.AuthService;
import com.sanbong.util.FeatureRowFactory;
import com.sanbong.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class RegisterController {

    @FXML
    private HBox feature1;
    @FXML
    private HBox feature2;
    @FXML
    private HBox feature3;
    @FXML
    private HBox feature4;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox venueOwnerCheckBox;
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
    private void handleRegister() {
        String fullName = trim(fullNameField.getText());
        String email = trim(emailField.getText());
        String phone = trim(phoneField.getText());
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        boolean asVenueOwner = venueOwnerCheckBox.isSelected();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ họ tên, email và mật khẩu.");
            return;
        }
        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }

        try {
            authService.register(fullName, email, password, phone, asVenueOwner);
            if (asVenueOwner) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Đăng ký thành công! Tài khoản chủ sân của bạn đang chờ quản trị viên phê duyệt, vui lòng quay lại đăng nhập sau.");
                alert.setHeaderText(null);
                alert.showAndWait();
            }
            SceneManager.showLogin();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (IOException e) {
            showError("Không thể mở màn hình đăng nhập.");
        }
    }

    @FXML
    private void goToLogin() {
        try {
            SceneManager.showLogin();
        } catch (IOException e) {
            showError("Không thể mở màn hình đăng nhập.");
        }
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }
}
