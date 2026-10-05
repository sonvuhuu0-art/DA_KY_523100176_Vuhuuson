package com.sanbong.controller;

import com.sanbong.util.NavIcon;
import com.sanbong.util.SceneManager;
import com.sanbong.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ShellController {

    @FXML
    private StackPane contentArea;
    @FXML
    private Label viewTitleLabel;
    @FXML
    private Label userAvatarLabel;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label userRoleLabel;

    @FXML
    private Button btnFieldList;
    @FXML
    private Button btnMyBookings;
    @FXML
    private Button btnProfile;
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnVenueManagement;
    @FXML
    private Button btnFieldManagement;
    @FXML
    private Button btnBookingManagement;
    @FXML
    private Button btnCustomerManagement;
    @FXML
    private Button btnVenueOwnerManagement;
    @FXML
    private Button btnLogout;
    @FXML
    private StackPane logoIconContainer;

    @FXML
    private void initialize() {
        logoIconContainer.getChildren().add(NavIcon.build("field"));

        Map<Button, String> icons = Map.of(
                btnFieldList, "list",
                btnMyBookings, "calendar",
                btnProfile, "user",
                btnDashboard, "chart",
                btnVenueManagement, "building",
                btnFieldManagement, "field",
                btnBookingManagement, "booking",
                btnCustomerManagement, "users",
                btnVenueOwnerManagement, "key");
        icons.forEach((button, icon) -> button.setGraphic(NavIcon.build(icon)));
        btnLogout.setGraphic(NavIcon.build("logout"));

        refreshUserLabel();

        boolean admin = SessionManager.isAdmin();
        boolean owner = SessionManager.isVenueOwner();
        boolean managesFields = admin || owner;
        boolean customer = !admin && !owner;

        setVisible(btnFieldList, customer);
        setVisible(btnMyBookings, customer);
        setVisible(btnProfile, true);
        setVisible(btnDashboard, managesFields);
        setVisible(btnVenueManagement, managesFields);
        setVisible(btnFieldManagement, managesFields);
        setVisible(btnBookingManagement, managesFields);
        setVisible(btnCustomerManagement, admin);
        setVisible(btnVenueOwnerManagement, admin);

        if (managesFields) {
            showDashboard();
        } else {
            showFieldList();
        }
    }

    public void refreshUserLabel() {
        var user = SessionManager.getCurrentUser();
        if (user == null) {
            userAvatarLabel.setText("");
            userNameLabel.setText("");
            userRoleLabel.setText("");
            return;
        }
        String fullName = user.getFullName() == null ? "" : user.getFullName().trim();
        userAvatarLabel.setText(fullName.isEmpty() ? "?" : fullName.substring(0, 1).toUpperCase());
        userNameLabel.setText(fullName);
        userRoleLabel.setText(roleLabel(user.getRole()));
        userRoleLabel.getStyleClass().removeAll("role-badge-admin", "role-badge-owner", "role-badge-customer");
        userRoleLabel.getStyleClass().add(switch (user.getRole()) {
            case "admin" -> "role-badge-admin";
            case "venue_owner" -> "role-badge-owner";
            default -> "role-badge-customer";
        });
    }

    private String roleLabel(String role) {
        return switch (role) {
            case "admin" -> "Quản trị viên";
            case "venue_owner" -> "Chủ sân";
            default -> "Khách hàng";
        };
    }

    private void setVisible(Button button, boolean visible) {
        button.setVisible(visible);
        button.setManaged(visible);
    }

    private void setActiveMenu(Button active) {
        for (Button b : List.of(btnFieldList, btnMyBookings, btnProfile, btnDashboard, btnVenueManagement,
                btnFieldManagement, btnBookingManagement, btnCustomerManagement, btnVenueOwnerManagement)) {
            b.getStyleClass().remove("menu-btn-active");
        }
        if (active != null) {
            active.getStyleClass().add("menu-btn-active");
        }
    }

    @FXML
    private void showFieldList() {
        showFieldListPublic();
    }

    public void showFieldListPublic() {
        FieldListController controller = load("/fxml/field_list.fxml", "Danh sách sân", btnFieldList);
        controller.setShell(this);
    }

    @FXML
    private void showMyBookings() {
        load("/fxml/my_bookings.fxml", "Lịch của tôi", btnMyBookings);
    }

    @FXML
    private void showProfile() {
        ProfileController controller = load("/fxml/profile.fxml", "Tài khoản của tôi", btnProfile);
        controller.setShell(this);
    }

    @FXML
    private void showDashboard() {
        load("/fxml/admin/dashboard.fxml", SessionManager.isAdmin() ? "Tổng quan hệ thống" : "Tổng quan sân của tôi", btnDashboard);
    }

    @FXML
    private void showVenueManagement() {
        load("/fxml/admin/venue_management.fxml", "Quản lý cụm sân", btnVenueManagement);
    }

    @FXML
    private void showFieldManagement() {
        load("/fxml/admin/field_management.fxml", "Quản lý sân", btnFieldManagement);
    }

    @FXML
    private void showBookingManagement() {
        load("/fxml/admin/booking_management.fxml", "Quản lý lịch đặt", btnBookingManagement);
    }

    @FXML
    private void showCustomerManagement() {
        load("/fxml/admin/customer_management.fxml", "Quản lý khách hàng", btnCustomerManagement);
    }

    @FXML
    private void showVenueOwnerManagement() {
        load("/fxml/admin/venue_owner_management.fxml", "Quản lý chủ sân", btnVenueOwnerManagement);
    }

    public void openFieldDetail(int fieldId) {
        FieldDetailController controller = load("/fxml/field_detail.fxml", "Chi tiết sân", btnFieldList);
        controller.setShell(this);
        controller.loadField(fieldId);
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        try {
            SceneManager.showLogin();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T load(String fxmlPath, String title, Button activeButton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            viewTitleLabel.setText(title);
            contentArea.getChildren().setAll(node);
            setActiveMenu(activeButton);
            return loader.getController();
        } catch (IOException e) {
            throw new RuntimeException("Không thể mở màn hình: " + fxmlPath, e);
        }
    }
}
