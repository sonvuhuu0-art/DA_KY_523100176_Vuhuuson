package com.sanbong.controller.admin;

import com.sanbong.dao.UserDao;
import com.sanbong.dao.VenueDao;
import com.sanbong.model.User;
import com.sanbong.model.Venue;
import com.sanbong.util.ImageStorage;
import com.sanbong.util.SessionManager;
import com.sanbong.util.ThumbnailFactory;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;

public class VenueManagementController {

    @FXML
    private TableView<Venue> venueTable;
    @FXML
    private TableColumn<Venue, String> colName;
    @FXML
    private TableColumn<Venue, String> colOwner;
    @FXML
    private TableColumn<Venue, String> colAddress;
    @FXML
    private TableColumn<Venue, String> colStatus;

    @FXML
    private ComboBox<User> ownerCombo;
    @FXML
    private TextField nameField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField phoneField;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextArea descriptionField;
    @FXML
    private StackPane imagePreview;
    @FXML
    private Label messageLabel;

    private final VenueDao venueDao = new VenueDao();
    private final UserDao userDao = new UserDao();
    private final boolean isAdmin = SessionManager.isAdmin();
    private Venue selectedVenue;
    private File pendingImageFile;

    @FXML
    private void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                "active".equals(data.getValue().getStatus()) ? "Đang hoạt động" : "Ngừng hoạt động"));

        statusCombo.setItems(FXCollections.observableArrayList("active", "inactive"));
        imagePreview.getChildren().setAll(ThumbnailFactory.build(null, 160, 90));

        ownerCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(User u) {
                return u == null ? "" : u.getFullName() + " (" + u.getEmail() + ")";
            }

            @Override
            public User fromString(String s) {
                return null;
            }
        });
        if (isAdmin) {
            ownerCombo.setItems(FXCollections.observableArrayList(userDao.findAllByRole("venue_owner")));
        } else {
            ownerCombo.setVisible(false);
            ownerCombo.setManaged(false);
        }

        venueTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedVenue = newVal;
            if (newVal != null) {
                populateForm(newVal);
            }
        });

        refresh();
    }

    private void refresh() {
        Integer ownerScope = isAdmin ? null : SessionManager.getCurrentUser().getId();
        venueTable.setItems(FXCollections.observableArrayList(venueDao.findAll(ownerScope)));
    }

    private void populateForm(Venue venue) {
        if (isAdmin) {
            ownerCombo.getItems().stream().filter(u -> u.getId() == venue.getOwnerId()).findFirst()
                    .ifPresent(ownerCombo.getSelectionModel()::select);
        }
        nameField.setText(venue.getName());
        addressField.setText(venue.getAddress());
        phoneField.setText(venue.getPhone());
        statusCombo.setValue(venue.getStatus());
        descriptionField.setText(venue.getDescription());
        pendingImageFile = null;
        imagePreview.getChildren().setAll(ThumbnailFactory.build(venue.getImage(), 160, 90));
        hideMessage();
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh cụm sân");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(imagePreview.getScene().getWindow());
        if (file == null) {
            return;
        }
        pendingImageFile = file;
        ImageView preview = new ImageView(new Image(file.toURI().toString(), 160, 90, false, true));
        imagePreview.getChildren().setAll(preview);
    }

    @FXML
    private void handleSave() {
        String name = trim(nameField.getText());
        String status = statusCombo.getValue();
        Integer ownerId = isAdmin
                ? (ownerCombo.getValue() != null ? ownerCombo.getValue().getId() : null)
                : SessionManager.getCurrentUser().getId();

        if (name.isEmpty() || status == null || ownerId == null) {
            showMessage(isAdmin
                    ? "Vui lòng nhập tên cụm sân, chọn chủ sân và trạng thái."
                    : "Vui lòng nhập tên cụm sân và chọn trạng thái.");
            return;
        }

        Venue venue = selectedVenue != null ? selectedVenue : new Venue();
        venue.setOwnerId(ownerId);
        venue.setName(name);
        venue.setAddress(trim(addressField.getText()));
        venue.setPhone(trim(phoneField.getText()));
        venue.setStatus(status);
        venue.setDescription(descriptionField.getText());
        if (pendingImageFile != null) {
            venue.setImage(ImageStorage.saveImage(pendingImageFile, "venue"));
        }

        if (selectedVenue == null) {
            venueDao.insert(venue);
        } else {
            venueDao.update(venue);
        }
        refresh();
        handleClearForm();
    }

    @FXML
    private void handleDelete() {
        if (selectedVenue == null) {
            showMessage("Vui lòng chọn một cụm sân để xóa.");
            return;
        }
        try {
            venueDao.delete(selectedVenue.getId());
            refresh();
            handleClearForm();
        } catch (RuntimeException e) {
            showMessage("Không thể xóa cụm sân này vì đang có sân trực thuộc.");
        }
    }

    @FXML
    private void handleClearForm() {
        selectedVenue = null;
        venueTable.getSelectionModel().clearSelection();
        if (isAdmin) {
            ownerCombo.getSelectionModel().clearSelection();
        }
        nameField.clear();
        addressField.clear();
        phoneField.clear();
        statusCombo.getSelectionModel().clearSelection();
        descriptionField.clear();
        pendingImageFile = null;
        imagePreview.getChildren().setAll(ThumbnailFactory.build(null, 160, 90));
        hideMessage();
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
    }

    private void hideMessage() {
        messageLabel.setManaged(false);
        messageLabel.setVisible(false);
    }
}
