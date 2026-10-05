package com.sanbong.controller.admin;

import com.sanbong.dao.UserDao;
import com.sanbong.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class CustomerManagementController {

    @FXML
    private TextField searchField;
    @FXML
    private TableView<User> customerTable;
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
    private List<User> allCustomers;

    @FXML
    private void initialize() {
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                "locked".equals(data.getValue().getStatus()) ? "Đã khóa" : "Đang hoạt động"));

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button toggleBtn = new Button();

            {
                toggleBtn.getStyleClass().add("btn-secondary");
                toggleBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    String newStatus = "locked".equals(user.getStatus()) ? "active" : "locked";
                    userDao.updateStatus(user.getId(), newStatus);
                    refresh();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                User user = getTableView().getItems().get(getIndex());
                toggleBtn.setText("locked".equals(user.getStatus()) ? "Mở khóa" : "Khóa tài khoản");
                setGraphic(toggleBtn);
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        refresh();
    }

    private void refresh() {
        allCustomers = userDao.findAllCustomers();
        applyFilter();
    }

    private void applyFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        var filtered = allCustomers.stream()
                .filter(u -> keyword.isEmpty()
                        || u.getFullName().toLowerCase().contains(keyword)
                        || u.getEmail().toLowerCase().contains(keyword))
                .toList();
        customerTable.setItems(FXCollections.observableArrayList(filtered));
    }
}
