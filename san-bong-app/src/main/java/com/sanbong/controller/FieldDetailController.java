package com.sanbong.controller;

import com.sanbong.dao.FieldDao;
import com.sanbong.dao.ReviewDao;
import com.sanbong.exception.SlotAlreadyBookedException;
import com.sanbong.model.Field;
import com.sanbong.model.TimeSlot;
import com.sanbong.service.BookingService;
import com.sanbong.util.DatePickerUtil;
import com.sanbong.util.SessionManager;
import com.sanbong.util.ThumbnailFactory;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class FieldDetailController {

    @FXML
    private StackPane imageContainer;
    @FXML
    private Label fieldNameLabel;
    @FXML
    private Label venueLabel;
    @FXML
    private Label ratingLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private DatePicker datePicker;
    @FXML
    private FlowPane slotContainer;
    @FXML
    private TextArea noteField;
    @FXML
    private Label messageLabel;

    private final FieldDao fieldDao = new FieldDao();
    private final BookingService bookingService = new BookingService();
    private final ReviewDao reviewDao = new ReviewDao();

    private ShellController shell;
    private Field field;
    private TimeSlot selectedSlot;
    private Button selectedButton;

    public void setShell(ShellController shell) {
        this.shell = shell;
    }

    public void loadField(int fieldId) {
        field = fieldDao.findById(fieldId).orElseThrow();
        fieldNameLabel.setText(field.getName());
        venueLabel.setText(field.getVenueName());
        descriptionLabel.setText(field.getDescription());

        imageContainer.getChildren().setAll(ThumbnailFactory.build(field.getImage(), 500, 220));

        double[] stats = reviewDao.averageAndCount(field.getId());
        int count = (int) stats[1];
        ratingLabel.setText(count == 0 ? "Chưa có đánh giá" : String.format("★ %.1f (%d đánh giá)", stats[0], count));

        datePicker.setValue(LocalDate.now());
        DatePickerUtil.restrictToFutureDates(datePicker);
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> refreshSlots());
        refreshSlots();
    }

    private void refreshSlots() {
        selectedSlot = null;
        selectedButton = null;
        slotContainer.getChildren().clear();
        hideMessage();

        LocalDate date = datePicker.getValue();
        if (date == null || field == null) {
            return;
        }
        List<TimeSlot> slots = bookingService.getAvailableSlots(field.getId(), date);
        for (TimeSlot slot : slots) {
            slotContainer.getChildren().add(buildSlotButton(slot));
        }
    }

    private VBox buildSlotButton(TimeSlot slot) {
        Button button = new Button(slot.getLabel() + "\n" + formatPrice(slot.getPrice()) + " đ");
        button.setWrapText(true);
        button.getStyleClass().add(slot.isBooked() ? "slot-booked" : "slot-free");
        button.setDisable(slot.isBooked());
        button.setOnAction(e -> selectSlot(slot, button));

        VBox wrapper = new VBox(button);
        return wrapper;
    }

    private void selectSlot(TimeSlot slot, Button button) {
        if (selectedButton != null) {
            selectedButton.getStyleClass().remove("slot-selected");
            selectedButton.getStyleClass().add("slot-free");
        }
        selectedSlot = slot;
        selectedButton = button;
        button.getStyleClass().remove("slot-free");
        button.getStyleClass().add("slot-selected");
        hideMessage();
    }

    @FXML
    private void handleBook() {
        if (selectedSlot == null) {
            showMessage("Vui lòng chọn một khung giờ trước khi đặt sân.");
            return;
        }
        var user = SessionManager.getCurrentUser();
        if (user == null) {
            showMessage("Phiên đăng nhập đã hết hạn.");
            return;
        }

        try {
            bookingService.createBooking(user.getId(), field.getId(), selectedSlot.getId(),
                    datePicker.getValue(), noteField.getText());
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đặt sân thành công! Vui lòng chờ xác nhận từ quản trị viên.");
            alert.setHeaderText(null);
            alert.showAndWait();
            refreshSlots();
        } catch (SlotAlreadyBookedException e) {
            showMessage(e.getMessage());
            refreshSlots();
        } catch (RuntimeException e) {
            showMessage("Đặt sân thất bại: " + e.getMessage());
        }
    }

    @FXML
    private void goBack() {
        if (shell != null) {
            shell.showFieldListPublic();
        }
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

    private String formatPrice(java.math.BigDecimal price) {
        return String.format("%,.0f", price);
    }
}
