package com.sanbong.util;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Resolves and opens the SQLite database file next to the running jar. */
public final class DatabaseConnection {

    private static final String DB_FILE_NAME = "data.db";
    private static String jdbcUrl;

    private DatabaseConnection() {
    }

    public static synchronized String getJdbcUrl() {
        if (jdbcUrl == null) {
            jdbcUrl = "jdbc:sqlite:" + resolveDbPath();
        }
        return jdbcUrl;
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(getJdbcUrl());
        try (var stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    private static String resolveDbPath() {
        File appDir = getAppDirectory();
        return new File(appDir, DB_FILE_NAME).getAbsolutePath();
    }

    public static File getAppDirectory() {
        try {
            File jarFile = new File(DatabaseConnection.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            File dir = jarFile.isFile() ? jarFile.getParentFile() : jarFile;
            if (dir != null && dir.exists()) {
                return dir;
            }
        } catch (URISyntaxException | NullPointerException ignored) {
            // fall through to working directory
        }
        return Paths.get("").toAbsolutePath().toFile();
    }
}
