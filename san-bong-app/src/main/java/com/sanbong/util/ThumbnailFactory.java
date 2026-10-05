package com.sanbong.util;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Builds an ImageView for a stored photo, or a placeholder tile when no photo is set. */
public final class ThumbnailFactory {

    private ThumbnailFactory() {
    }

    public static Region build(String relativeImagePath, double width, double height) {
        Image image = ImageStorage.resolve(relativeImagePath);
        StackPane pane = new StackPane();
        pane.setPrefSize(width, height);
        pane.setMinSize(width, height);
        pane.setMaxSize(width, height);

        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setPreserveRatio(false);
            pane.getChildren().add(imageView);
        } else {
            pane.getStyleClass().add("image-placeholder");
            Label icon = new Label("⚽");
            icon.getStyleClass().add("image-placeholder-icon");
            pane.getChildren().add(icon);
        }
        return pane;
    }
}
