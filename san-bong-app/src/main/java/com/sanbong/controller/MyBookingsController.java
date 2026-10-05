package com.sanbong.controller;

import com.sanbong.dao.ReviewDao;
import com.sanbong.model.Booking;
import com.sanbong.model.Review;
import com.sanbong.service.BookingService;
import com.sanbong.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.Optional;

public class MyBookingsController {

    @FXML
    private TableView<Booking> bookingTable;
    @FXML
    private TableColumn<Booking, String> colField;
    @FXML
    private TableColumn<Booking, String> colVenue;
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
    @FXML
    private Label emptyLabel;

    private final BookingService bookingService = new BookingService();
    private final ReviewDao reviewDao = new ReviewDao();

    @FXML
    private void initialize() {
        colField.setCellValueFactory(new PropertyValueFactory<>("fieldName"));
        colVenue.setCellValueFactory(new PropertyValueFactory<>("venueName"));
        colDate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getBookingDate().toString()));
        colSlot.setCellValueFactory(new PropertyValueFactory<>("slotLabel"));
        colPrice.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.format("%,.0f đ", data.getValue().getTotalPrice())));
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                statusLabel(data.getValue().getStatus())));
        colPayment.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                paymentLabel(data.getValue().getPaymentStatus())));

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button cancelButton = new Button("Hủy lịch");
            private final Button reviewButton = new Button("Đánh giá");
            private final HBox box = new HBox(6, cancelButton, reviewButton);

            {
                cancelButton.getStyleClass().add("btn-secondary");
                reviewButton.getStyleClass().add("btn-secondary");
                cancelButton.setOnAction(e -> handleCancel(current()));
                reviewButton.setOnAction(e -> handleReview(current()));
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
                Booking booking = current();
                boolean cancellable = "pending".equals(booking.getStatus()) || "confirmed".equals(booking.getStatus());
                cancelButton.setVisible(cancellable);
                cancelButton.setManaged(cancellable);

                boolean reviewable = "completed".equals(booking.getStatus())
                        && !reviewDao.existsByUserAndField(booking.getUserId(), booking.getFieldId());
                reviewButton.setVisible(reviewable);
                reviewButton.setManaged(reviewable);

                setGraphic((cancellable || reviewable) ? box : null);
            }
        });

        loadBookings();
    }

    private void loadBookings() {
        var user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }
        var bookings = bookingService.getMyBookings(user.getId());
        bookingTable.setItems(FXCollections.observableArrayList(bookings));
        boolean empty = bookings.isEmpty();
        emptyLabel.setManaged(empty);
        emptyLabel.setVisible(empty);
    }

    private void handleCancel(Booking booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn hủy lịch đặt sân " + booking.getFieldName() + " ngày " + booking.getBookingDate() + "?");
        confirm.setHeaderText(null);
        var result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            bookingService.cancelBooking(booking.getId());
            loadBookings();
        } catch (IllegalStateException | IllegalArgumentException e) {
            Alert error = new Alert(Alert.AlertType.ERROR, e.getMessage());
            error.setHeaderText(null);
            error.showAndWait();
        }
    }

    private void handleReview(Booking booking) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Đánh giá sân");
        dialog.setHeaderText("Đánh giá " + booking.getFieldName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ChoiceBox<Integer> ratingChoice = new ChoiceBox<>(FXCollections.observableArrayList(5, 4, 3, 2, 1));
        ratingChoice.getSelectionModel().selectFirst();
        TextArea commentField = new TextArea();
        commentField.setPromptText("Cảm nhận của bạn về sân (không bắt buộc)");
        commentField.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Số sao (1-5):"), ratingChoice);
        grid.addRow(1, new Label("Nhận xét:"), commentField);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            var user = SessionManager.getCurrentUser();
            Review review = new Review(user.getId(), booking.getFieldId(), ratingChoice.getValue(), commentField.getText());
            reviewDao.insert(review);
            bookingTable.refresh();
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
