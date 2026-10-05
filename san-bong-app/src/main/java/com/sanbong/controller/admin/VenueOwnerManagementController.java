package com.sanbong.controller.admin;

import com.sanbong.dao.UserDao;
import com.sanbong.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.List;

public class VenueOwnerManagementController {

    @FXML
    private TextField searchField;
    @FXML
    private TableView<User> ownerTable;
    @FXML
    private TableColumn<User, String> colFullName;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colPhone;
    @FXML
    private TableColumn<User, String> colStatus;
    @FXML
    private TableColumn<User, Void> colAction;

    private final UserDao userDao = new UserDao();
    private List<User> allOwners;

    @FXML
    private void initialize() {
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(statusLabel(data.getValue().getStatus())));

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button approveBtn = new Button("Duyệt");
            private final Button toggleBtn = new Button();
            private final HBox box = new HBox(6, approveBtn, toggleBtn);

            {
                approveBtn.getStyleClass().add("btn-primary");
                toggleBtn.getStyleClass().add("btn-secondary");
                approveBtn.setOnAction(e -> {
                    userDao.updateStatus(current().getId(), "active");
                    refresh();
                });
                toggleBtn.setOnAction(e -> {
                    User user = current();
                    String newStatus = "locked".equals(user.getStatus()) ? "active" : "locked";
                    userDao.updateStatus(user.getId(), newStatus);
                    refresh();
                });
            }

            private User current() {
                return getTableView().getItems().get(getIndex());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                User user = current();
                boolean pending = "pending".equals(user.getStatus());
                approveBtn.setVisible(pending);
                approveBtn.setManaged(pending);
                toggleBtn.setVisible(!pending);
                toggleBtn.setManaged(!pending);
                toggleBtn.setText("locked".equals(user.getStatus()) ? "Mở khóa" : "Khóa tài khoản");
                setGraphic(box);
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        refresh();
    }

    private void refresh() {
        allOwners = userDao.findAllByRole("venue_owner");
        applyFilter();
    }

    private void applyFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        var filtered = allOwners.stream()
                .filter(u -> keyword.isEmpty()
                        || u.getFullName().toLowerCase().contains(keyword)
                        || u.getEmail().toLowerCase().contains(keyword))
                .toList();
        ownerTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "pending" -> "Chờ duyệt";
            case "locked" -> "Đã khóa";
            default -> "Đang hoạt động";
        };
    }
}
