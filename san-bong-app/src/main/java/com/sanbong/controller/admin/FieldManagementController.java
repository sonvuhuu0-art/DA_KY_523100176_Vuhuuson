package com.sanbong.controller.admin;

import com.sanbong.dao.FieldDao;
import com.sanbong.dao.TimeSlotDao;
import com.sanbong.dao.VenueDao;
import com.sanbong.model.Field;
import com.sanbong.model.TimeSlot;
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
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class FieldManagementController {

    @FXML
    private TableView<Field> fieldTable;
    @FXML
    private TableColumn<Field, String> colName;
    @FXML
    private TableColumn<Field, String> colVenue;
    @FXML
    private TableColumn<Field, String> colType;
    @FXML
    private TableColumn<Field, String> colStatus;

    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<Venue> venueCombo;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextArea descriptionField;
    @FXML
    private StackPane imagePreview;
    @FXML
    private Label fieldMessageLabel;

    @FXML
    private Label slotSectionLabel;
    @FXML
    private TableView<TimeSlot> slotTable;
    @FXML
    private TableColumn<TimeSlot, String> colSlotTime;
    @FXML
    private TableColumn<TimeSlot, String> colSlotDayType;
    @FXML
    private TableColumn<TimeSlot, String> colSlotPrice;

    @FXML
    private TextField startTimeField;
    @FXML
    private TextField endTimeField;
    @FXML
    private ComboBox<String> dayTypeCombo;
    @FXML
    private TextField priceField;
    @FXML
    private Label slotMessageLabel;

    private final FieldDao fieldDao = new FieldDao();
    private final VenueDao venueDao = new VenueDao();
    private final TimeSlotDao timeSlotDao = new TimeSlotDao();

    private Field selectedField;
    private TimeSlot selectedSlot;
    private File pendingImageFile;

    @FXML
    private void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colVenue.setCellValueFactory(new PropertyValueFactory<>("venueName"));
        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType() + " người"));
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(statusLabel(data.getValue().getStatus())));

        colSlotTime.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getLabel()));
        colSlotDayType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                "weekend".equals(data.getValue().getDayType()) ? "Cuối tuần" : "Ngày thường"));
        colSlotPrice.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.format("%,.0f đ", data.getValue().getPrice())));

        typeCombo.setItems(FXCollections.observableArrayList("5", "7", "11"));
        statusCombo.setItems(FXCollections.observableArrayList("active", "maintenance", "inactive"));
        dayTypeCombo.setItems(FXCollections.observableArrayList("weekday", "weekend"));

        venueCombo.setItems(FXCollections.observableArrayList(venueDao.findAll(SessionManager.ownerScope())));
        imagePreview.getChildren().setAll(ThumbnailFactory.build(null, 160, 90));

        fieldTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedField = newVal;
            if (newVal != null) {
                populateFieldForm(newVal);
                loadSlots(newVal);
            }
        });

        slotTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedSlot = newVal;
            if (newVal != null) {
                populateSlotForm(newVal);
            }
        });

        refreshFields();
    }

    private void refreshFields() {
        List<Field> fields = fieldDao.findAll(SessionManager.ownerScope());
        fieldTable.setItems(FXCollections.observableArrayList(fields));
    }

    private void populateFieldForm(Field field) {
        nameField.setText(field.getName());
        venueCombo.getItems().stream().filter(v -> v.getId() == field.getVenueId()).findFirst()
                .ifPresent(venueCombo.getSelectionModel()::select);
        typeCombo.setValue(field.getType());
        statusCombo.setValue(field.getStatus());
        descriptionField.setText(field.getDescription());
        pendingImageFile = null;
        imagePreview.getChildren().setAll(ThumbnailFactory.build(field.getImage(), 160, 90));
        hideFieldMessage();
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh sân");
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
        Venue venue = venueCombo.getValue();
        String type = typeCombo.getValue();
        String status = statusCombo.getValue();
        String description = descriptionField.getText();

        if (name.isEmpty() || venue == null || type == null || status == null) {
            showFieldMessage("Vui lòng nhập đầy đủ tên sân, cụm sân, loại sân và trạng thái.");
            return;
        }

        Field field = selectedField != null ? selectedField : new Field();
        field.setName(name);
        field.setVenueId(venue.getId());
        field.setType(type);
        field.setStatus(status);
        field.setDescription(description);
        if (pendingImageFile != null) {
            field.setImage(ImageStorage.saveImage(pendingImageFile, "field"));
        }

        if (selectedField == null) {
            fieldDao.insert(field);
        } else {
            fieldDao.update(field);
        }
        refreshFields();
        handleClearForm();
    }

    @FXML
    private void handleDelete() {
        if (selectedField == null) {
            showFieldMessage("Vui lòng chọn một sân để xóa.");
            return;
        }
        try {
            fieldDao.delete(selectedField.getId());
            refreshFields();
            handleClearForm();
        } catch (RuntimeException e) {
            showFieldMessage("Không thể xóa sân này vì đang có khung giờ hoặc lịch đặt liên kết.");
        }
    }

    @FXML
    private void handleClearForm() {
        selectedField = null;
        fieldTable.getSelectionModel().clearSelection();
        nameField.clear();
        venueCombo.getSelectionModel().clearSelection();
        typeCombo.getSelectionModel().clearSelection();
        statusCombo.getSelectionModel().clearSelection();
        descriptionField.clear();
        pendingImageFile = null;
        imagePreview.getChildren().setAll(ThumbnailFactory.build(null, 160, 90));
        slotTable.setItems(FXCollections.observableArrayList());
        slotSectionLabel.setText("Khung giờ & giá — chọn một sân ở bên trái");
        handleClearSlotForm();
        hideFieldMessage();
    }

    private void loadSlots(Field field) {
        slotSectionLabel.setText("Khung giờ & giá — " + field.getName());
        slotTable.setItems(FXCollections.observableArrayList(timeSlotDao.findByFieldId(field.getId())));
    }

    private void populateSlotForm(TimeSlot slot) {
        startTimeField.setText(slot.getStartTime().toString());
        endTimeField.setText(slot.getEndTime().toString());
        dayTypeCombo.setValue(slot.getDayType());
        priceField.setText(slot.getPrice().toPlainString());
        hideSlotMessage();
    }

    @FXML
    private void handleSaveSlot() {
        if (selectedField == null) {
            showSlotMessage("Vui lòng chọn một sân trước khi thêm khung giờ.");
            return;
        }
        LocalTime start;
        LocalTime end;
        BigDecimal price;
        try {
            start = LocalTime.parse(trim(startTimeField.getText()));
            end = LocalTime.parse(trim(endTimeField.getText()));
        } catch (DateTimeParseException e) {
            showSlotMessage("Giờ phải theo định dạng HH:mm, ví dụ 18:00.");
            return;
        }
        String dayType = dayTypeCombo.getValue();
        if (dayType == null) {
            showSlotMessage("Vui lòng chọn loại ngày (ngày thường/cuối tuần).");
            return;
        }
        if (!end.isAfter(start)) {
            showSlotMessage("Giờ kết thúc phải sau giờ bắt đầu.");
            return;
        }
        try {
            price = new BigDecimal(trim(priceField.getText()));
        } catch (NumberFormatException e) {
            showSlotMessage("Giá phải là một số hợp lệ.");
            return;
        }

        TimeSlot slot = selectedSlot != null ? selectedSlot : new TimeSlot();
        slot.setFieldId(selectedField.getId());
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setDayType(dayType);
        slot.setPrice(price);

        if (selectedSlot == null) {
            timeSlotDao.insert(slot);
        } else {
            timeSlotDao.update(slot);
        }
        loadSlots(selectedField);
        handleClearSlotForm();
    }

    @FXML
    private void handleDeleteSlot() {
        if (selectedSlot == null) {
            showSlotMessage("Vui lòng chọn một khung giờ để xóa.");
            return;
        }
        try {
            timeSlotDao.delete(selectedSlot.getId());
            loadSlots(selectedField);
            handleClearSlotForm();
        } catch (RuntimeException e) {
            showSlotMessage("Không thể xóa khung giờ này vì đang có lịch đặt liên kết.");
        }
    }

    private void handleClearSlotForm() {
        selectedSlot = null;
        slotTable.getSelectionModel().clearSelection();
        startTimeField.clear();
        endTimeField.clear();
        dayTypeCombo.getSelectionModel().clearSelection();
        priceField.clear();
        hideSlotMessage();
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "active" -> "Đang hoạt động";
            case "maintenance" -> "Đang bảo trì";
            default -> "Ngừng hoạt động";
        };
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private void showFieldMessage(String message) {
        fieldMessageLabel.setText(message);
        fieldMessageLabel.setManaged(true);
        fieldMessageLabel.setVisible(true);
    }

    private void hideFieldMessage() {
        fieldMessageLabel.setManaged(false);
        fieldMessageLabel.setVisible(false);
    }

    private void showSlotMessage(String message) {
        slotMessageLabel.setText(message);
        slotMessageLabel.setManaged(true);
        slotMessageLabel.setVisible(true);
    }

    private void hideSlotMessage() {
        slotMessageLabel.setManaged(false);
        slotMessageLabel.setVisible(false);
    }
}
