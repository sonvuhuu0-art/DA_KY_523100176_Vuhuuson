package com.sanbong.controller;

import com.sanbong.dao.FieldDao;
import com.sanbong.dao.ReviewDao;
import com.sanbong.dao.VenueDao;
import com.sanbong.model.Field;
import com.sanbong.model.Venue;
import com.sanbong.util.ThumbnailFactory;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class FieldListController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> typeFilter;
    @FXML
    private ComboBox<Venue> venueFilter;
    @FXML
    private FlowPane cardContainer;

    private static final Venue ALL_VENUES = new Venue();

    private final FieldDao fieldDao = new FieldDao();
    private final VenueDao venueDao = new VenueDao();
    private final ReviewDao reviewDao = new ReviewDao();
    private List<Field> allFields;
    private ShellController shell;

    static {
        ALL_VENUES.setId(-1);
        ALL_VENUES.setName("Tất cả cụm sân");
    }

    @FXML
    private void initialize() {
        typeFilter.setItems(FXCollections.observableArrayList("Tất cả", "5 người", "7 người", "11 người"));
        typeFilter.getSelectionModel().selectFirst();

        List<Venue> venueOptions = new ArrayList<>();
        venueOptions.add(ALL_VENUES);
        venueOptions.addAll(venueDao.findAll());
        venueFilter.setItems(FXCollections.observableArrayList(venueOptions));
        venueFilter.getSelectionModel().selectFirst();

        allFields = fieldDao.findAll();
        renderCards(allFields);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        venueFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    public void setShell(ShellController shell) {
        this.shell = shell;
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String typeLabel = typeFilter.getValue();
        Venue venue = venueFilter.getValue();

        List<Field> filtered = allFields.stream()
                .filter(f -> keyword.isEmpty() || f.getName().toLowerCase().contains(keyword)
                        || f.getVenueName().toLowerCase().contains(keyword))
                .filter(f -> typeLabel == null || "Tất cả".equals(typeLabel) || typeLabel.startsWith(f.getType()))
                .filter(f -> venue == null || venue.getId() == -1 || venue.getId() == f.getVenueId())
                .toList();
        renderCards(filtered);
    }

    private void renderCards(List<Field> fields) {
        cardContainer.getChildren().clear();
        for (Field field : fields) {
            cardContainer.getChildren().add(buildCard(field));
        }
    }

    private VBox buildCard(Field field) {
        VBox card = new VBox(6);
        card.getStyleClass().add("field-card");
        card.setAlignment(Pos.TOP_LEFT);

        var thumbnail = ThumbnailFactory.build(field.getImage(), 220, 120);

        Label title = new Label(field.getName());
        title.getStyleClass().add("field-card-title");

        Label venue = new Label(field.getVenueName());
        venue.getStyleClass().add("field-card-sub");

        Label type = new Label("Loại: sân " + field.getType() + " người");
        type.getStyleClass().add("badge");

        Label status = new Label(statusLabel(field.getStatus()));
        status.getStyleClass().add("field-card-sub");

        Label rating = new Label(ratingLabel(field.getId()));
        rating.getStyleClass().add("field-card-sub");

        card.getChildren().addAll(thumbnail, title, venue, type, rating, status);
        card.setOnMouseClicked(e -> {
            if (shell != null) {
                shell.openFieldDetail(field.getId());
            }
        });
        return card;
    }

    private String ratingLabel(int fieldId) {
        double[] stats = reviewDao.averageAndCount(fieldId);
        int count = (int) stats[1];
        if (count == 0) {
            return "Chưa có đánh giá";
        }
        return String.format("★ %.1f (%d đánh giá)", stats[0], count);
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "active" -> "Đang hoạt động";
            case "maintenance" -> "Đang bảo trì";
            default -> "Ngừng hoạt động";
        };
    }
}
