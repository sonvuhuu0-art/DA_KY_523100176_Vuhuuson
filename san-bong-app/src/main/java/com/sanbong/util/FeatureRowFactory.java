package com.sanbong.util;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Builds the icon + title/description rows shown on the login/register branding panel. */
public final class FeatureRowFactory {

    private FeatureRowFactory() {
    }

    public static void fill(HBox container, String iconName, String title, String description) {
        StackPane badge = new StackPane(NavIcon.build(iconName));
        badge.getStyleClass().add("feature-icon-badge");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("feature-title");
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("feature-desc");
        descLabel.setWrapText(true);
        descLabel.setPrefWidth(360);
        descLabel.setMaxWidth(360);

        VBox textBox = new VBox(2, titleLabel, descLabel);
        container.getChildren().setAll(badge, textBox);
    }
}
