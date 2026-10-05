package com.sanbong.controller.admin;

import com.sanbong.dao.FieldDao;
import com.sanbong.dao.UserDao;
import com.sanbong.exception.SlotAlreadyBookedException;
import com.sanbong.model.Booking;
import com.sanbong.model.Field;
import com.sanbong.model.TimeSlot;
import com.sanbong.model.User;
import com.sanbong.service.BookingService;
import com.sanbong.service.PaymentService;
import com.sanbong.util.DatePickerUtil;
import com.sanbong.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BookingManagementController {

    @FXML
    private TextField searchField;
    @FXML
    private DatePicker dateFilter;
    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private TableView<Booking> bookingTable;
    @FXML
    private TableColumn<Booking, String> colCustomer;
    @FXML
    private TableColumn<Booking, String> colField;
    @FXML
    private TableColumn<Booking, String> colDate;
    @FXML
    private TableColumn<Booking, String> colSlot;
    @FXML
    private TableColumn<Booking, String> colPrice;
    @FXML
    private TableColumn<Booking, String> colStatus;
    @FXML
    private TableColumn<Booking, String> colPayment;
    @FXML
    private TableColumn<Booking, Void> colAction;

    private final BookingService bookingService = new BookingService();
    private final PaymentService paymentService = new PaymentService();
    private final UserDao userDao = new UserDao();
    private final FieldDao fieldDao = new FieldDao();
    private List<Booking> allBookings;

    @FXML
    private void initialize() {
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colField.setCellValueFactory(new PropertyValueFactory<>("fieldName"));
        colDate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getBookingDate().toString()));
        colSlot.setCellValueFactory(new PropertyValueFactory<>("slotLabel"));
        colPrice.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.format("%,.0f đ", data.getValue().getTotalPrice())));
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(statusLabel(data.getValue().getStatus())));
        colPayment.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(paymentLabel(data.getValue().getPaymentStatus())));

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button confirmBtn = new Button("Xác nhận");
            private final Button cancelBtn = new Button("Hủy");
            private final Button completeBtn = new Button("Hoàn thành");
            private final Button payBtn = new Button("Ghi nhận TT");
            private final HBox box = new HBox(6, confirmBtn, cancelBtn, completeBtn, payBtn);

            {
                confirmBtn.getStyleClass().add("btn-secondary");
                cancelBtn.getStyleClass().add("btn-secondary");
                completeBtn.getStyleClass().add("btn-secondary");
                payBtn.getStyleClass().add("btn-secondary");
                confirmBtn.setOnAction(e -> {
                    if (confirmAction("Xác nhận lịch đặt " + current().getFieldName() + " ngày " + current().getBookingDate() + "?")) {
                        bookingService.confirmBooking(current().getId());
                        refresh();
                    }
                });
                cancelBtn.setOnAction(e -> {
                    if (confirmAction("Hủy lịch đặt " + current().getFieldName() + " ngày " + current().getBookingDate() + "?")) {
                        bookingService.adminCancelBooking(current().getId());
                        refresh();
                    }
                });
                completeBtn.setOnAction(e -> {
                    if (confirmAction("Đánh dấu lịch đặt " + current().getFieldName() + " ngày " + current().getBookingDate() + " là đã hoàn thành?")) {
                        bookingService.markCompleted(current().getId());
                        refresh();
                    }
                });
                payBtn.setOnAction(e -> openPaymentDialog(current()));
            }

            private Booking current() {
                return getTableView().getItems().get(getIndex());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Booking b = getTableView().getItems().get(getIndex());
                confirmBtn.setDisable(!"pending".equals(b.getStatus()));
                cancelBtn.setDisable("cancelled".equals(b.getStatus()) || "completed".equals(b.getStatus()));
                completeBtn.setDisable(!"confirmed".equals(b.getStatus()));
                payBtn.setDisable("paid".equals(b.getPaymentStatus()) || "cancelled".equals(b.getStatus()));
                setGraphic(box);
            }
        });

        statusFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "Chờ xác nhận", "Đã xác nhận", "Đã hủy", "Hoàn thành"));
        statusFilter.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        refresh();
    }

    private void refresh() {
        allBookings = bookingService.getAllBookings(SessionManager.ownerScope());
        applyFilters();
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        var date = dateFilter.getValue();
        String statusLabel = statusFilter.getValue();

        var filtered = allBookings.stream()
                .filter(b -> keyword.isEmpty()
                        || b.getCustomerName().toLowerCase().contains(keyword)
                        || b.getFieldName().toLowerCase().contains(keyword))
                .filter(b -> date == null || b.getBookingDate().equals(date))
                .filter(b -> statusLabel == null || "Tất cả".equals(statusLabel) || statusLabel.equals(statusLabel(b.getStatus())))
                .toList();
        bookingTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleClearFilter() {
        searchField.clear();
        dateFilter.setValue(null);
        statusFilter.getSelectionModel().selectFirst();
        refresh();
    }

    @FXML
    private void handleWalkInBooking() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Đặt hộ khách");
        dialog.setHeaderText("Tạo lịch đặt cho khách hàng đến trực tiếp / gọi điện");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<User> customerCombo = new ComboBox<>(FXCollections.observableArrayList(userDao.findAllCustomers()));
        customerCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(User u) {
                return u == null ? "" : u.getFullName() + " (" + u.getEmail() + ")";
            }

            @Override
            public User fromString(String s) {
                return null;
            }
        });

        ComboBox<Field> fieldCombo = new ComboBox<>(FXCollections.observableArrayList(fieldDao.findAll(SessionManager.ownerScope())));
        DatePicker datePicker = new DatePicker(LocalDate.now());
        DatePickerUtil.restrictToFutureDates(datePicker);
        ComboBox<TimeSlot> slotCombo = new ComboBox<>();
        slotCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(TimeSlot s) {
                return s == null ? "" : s.getLabel() + " — " + String.format("%,.0f đ", s.getPrice());
            }

            @Override
            public TimeSlot fromString(String s) {
                return null;
            }
        });
        TextField noteField = new TextField();
        noteField.setPromptText("Ghi chú (không bắt buộc)");
        Label warningLabel = new Label();
        warningLabel.getStyleClass().add("error-label");
        warningLabel.setWrapText(true);

        Runnable refreshSlots = () -> {
            slotCombo.getItems().clear();
            Field field = fieldCombo.getValue();
            LocalDate date = datePicker.getValue();
            if (field == null || date == null) {
                return;
            }
            List<TimeSlot> available = bookingService.getAvailableSlots(field.getId(), date).stream()
                    .filter(s -> !s.isBooked())
                    .toList();
            slotCombo.setItems(FXCollections.observableArrayList(available));
        };
        fieldCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshSlots.run());
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> refreshSlots.run());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Khách hàng:"), customerCombo);
        grid.addRow(1, new Label("Sân:"), fieldCombo);
        grid.addRow(2, new Label("Ngày:"), datePicker);
        grid.addRow(3, new Label("Khung giờ:"), slotCombo);
        grid.addRow(4, new Label("Ghi chú:"), noteField);
        grid.add(warningLabel, 1, 5);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        User customer = customerCombo.getValue();
        Field field = fieldCombo.getValue();
        TimeSlot slot = slotCombo.getValue();
        LocalDate date = datePicker.getValue();
        if (customer == null || field == null || slot == null || date == null) {
            showError("Vui lòng chọn đầy đủ khách hàng, sân, ngày và khung giờ.");
            return;
        }
        try {
            Booking booking = bookingService.createBooking(customer.getId(), field.getId(), slot.getId(), date, noteField.getText());
            bookingService.confirmBooking(booking.getId());
            refresh();
        } catch (SlotAlreadyBookedException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private boolean confirmAction(String message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, message);
        confirm.setHeaderText(null);
        var result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void openPaymentDialog(Booking booking) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ghi nhận thanh toán");
        dialog.setHeaderText("Lịch đặt: " + booking.getFieldName() + " - " + booking.getBookingDate());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField amountField = new TextField(booking.getTotalPrice().toPlainString());
        ComboBox<String> methodCombo = new ComboBox<>(FXCollections.observableArrayList("cash", "bank_transfer"));
        methodCombo.getSelectionModel().selectFirst();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Số tiền:"), amountField);
        grid.addRow(1, new Label("Hình thức:"), methodCombo);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                BigDecimal amount = new BigDecimal(amountField.getText().trim());
                paymentService.recordPayment(booking.getId(), amount, methodCombo.getValue());
                refresh();
            } catch (NumberFormatException e) {
                showError("Số tiền không hợp lệ.");
            }
        }
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "pending" -> "Chờ xác nhận";
            case "confirmed" -> "Đã xác nhận";
            case "cancelled" -> "Đã hủy";
            case "completed" -> "Hoàn thành";
            default -> status;
        };
    }

    private String paymentLabel(String status) {
        return switch (status) {
            case "unpaid" -> "Chưa thanh toán";
            case "deposited" -> "Đã cọc";
            case "paid" -> "Đã thanh toán";
            default -> status;
        };
    }
}
