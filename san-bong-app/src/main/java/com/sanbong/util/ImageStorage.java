package com.sanbong.util;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Copies user-picked field/venue photos into an "images" folder next to
 * data.db, so the whole app (jar + data.db + images) stays copy-portable
 * to another machine instead of referencing files from wherever they were
 * originally picked.
 */
public final class ImageStorage {

    private static final String IMAGES_DIR_NAME = "images";

    private ImageStorage() {
    }

    /** Copies the source file into the app's images folder and returns a path relative to the app directory. */
    public static String saveImage(File source, String prefix) {
        try {
            File imagesDir = new File(DatabaseConnection.getAppDirectory(), IMAGES_DIR_NAME);
            Files.createDirectories(imagesDir.toPath());

            String extension = extensionOf(source.getName());
            String fileName = prefix + "_" + System.currentTimeMillis() + extension;
            Path target = imagesDir.toPath().resolve(fileName);
            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

            return IMAGES_DIR_NAME + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu ảnh: " + e.getMessage(), e);
        }
    }

    /** Loads an image previously saved via {@link #saveImage}, or null if the path is blank/missing. */
    public static Image resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        File file = new File(DatabaseConnection.getAppDirectory(), relativePath);
        if (!file.exists()) {
            return null;
        }
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return new Image(in);
        } catch (IOException e) {
            return null;
        }
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }
}
