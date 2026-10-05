package com.sanbong.controller;

import com.sanbong.dao.UserDao;
import com.sanbong.service.AuthService;
import com.sanbong.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ProfileController {

    @FXML
    private TextField emailField;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private Label profileMessageLabel;

    @FXML
    private PasswordField oldPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label passwordMessageLabel;

    private final UserDao userDao = new UserDao();
    private final AuthService authService = new AuthService();
    private ShellController shell;

    @FXML
    private void initialize() {
        var user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }
        emailField.setText(user.getEmail());
        fullNameField.setText(user.getFullName());
        phoneField.setText(user.getPhone());
    }

    public void setShell(ShellController shell) {
        this.shell = shell;
    }

    @FXML
    private void handleSaveProfile() {
        var user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }
        String fullName = trim(fullNameField.getText());
        String phone = trim(phoneField.getText());
        if (fullName.isEmpty()) {
            showProfileMessage("Họ tên không được để trống.");
            return;
        }

        userDao.updateProfile(user.getId(), fullName, phone);
        user.setFullName(fullName);
        user.setPhone(phone);
        if (shell != null) {
            shell.refreshUserLabel();
        }
        showProfileMessage("Đã lưu thông tin cá nhân.");
    }

    @FXML
    private void handleChangePassword() {
        var user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }
        String oldPassword = oldPasswordField.getText() == null ? "" : oldPasswordField.getText();
        String newPassword = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (!newPassword.equals(confirmPassword)) {
            showPasswordMessage("Mật khẩu mới nhập lại không khớp.");
            return;
        }

        try {
            authService.changePassword(user.getId(), oldPassword, newPassword);
            oldPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            showPasswordMessage("Đổi mật khẩu thành công.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            showPasswordMessage(e.getMessage());
        }
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private void showProfileMessage(String message) {
        profileMessageLabel.setText(message);
        profileMessageLabel.setManaged(true);
        profileMessageLabel.setVisible(true);
    }

    private void showPasswordMessage(String message) {
        passwordMessageLabel.setText(message);
        passwordMessageLabel.setManaged(true);
        passwordMessageLabel.setVisible(true);
    }
}
