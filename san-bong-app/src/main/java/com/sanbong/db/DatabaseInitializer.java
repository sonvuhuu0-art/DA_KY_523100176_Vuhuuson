package com.sanbong.db;

import com.sanbong.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates every table (if missing) on first application start. */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    phone VARCHAR(15),
                    role VARCHAR(15) NOT NULL DEFAULT 'customer' CHECK (role IN ('customer','venue_owner','admin')),
                    avatar VARCHAR(255),
                    status VARCHAR(15) NOT NULL DEFAULT 'active' CHECK (status IN ('active','locked','pending')),
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS venues (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_id INTEGER REFERENCES users(id),
                    name VARCHAR(100) NOT NULL,
                    address VARCHAR(255),
                    phone VARCHAR(15),
                    description TEXT,
                    image VARCHAR(255),
                    status VARCHAR(10) NOT NULL DEFAULT 'active' CHECK (status IN ('active','inactive')),
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS fields (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    venue_id INTEGER NOT NULL REFERENCES venues(id),
                    name VARCHAR(100) NOT NULL,
                    type VARCHAR(2) NOT NULL CHECK (type IN ('5','7','11')),
                    description TEXT,
                    status VARCHAR(15) NOT NULL DEFAULT 'active' CHECK (status IN ('active','maintenance','inactive')),
                    image VARCHAR(255),
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS time_slots (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    field_id INTEGER NOT NULL REFERENCES fields(id),
                    start_time TIME NOT NULL,
                    end_time TIME NOT NULL,
                    day_type VARCHAR(10) NOT NULL CHECK (day_type IN ('weekday','weekend')),
                    price DECIMAL(10,2) NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bookings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL REFERENCES users(id),
                    field_id INTEGER NOT NULL REFERENCES fields(id),
                    time_slot_id INTEGER NOT NULL REFERENCES time_slots(id),
                    booking_date DATE NOT NULL,
                    total_price DECIMAL(10,2) NOT NULL,
                    status VARCHAR(15) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','confirmed','cancelled','completed')),
                    payment_status VARCHAR(10) NOT NULL DEFAULT 'unpaid' CHECK (payment_status IN ('unpaid','deposited','paid')),
                    note VARCHAR(255),
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE (field_id, time_slot_id, booking_date)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_id INTEGER NOT NULL REFERENCES bookings(id),
                    amount DECIMAL(10,2) NOT NULL,
                    method VARCHAR(15) NOT NULL CHECK (method IN ('cash','bank_transfer')),
                    status VARCHAR(10) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','success','failed')),
                    paid_at DATETIME
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reviews (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL REFERENCES users(id),
                    field_id INTEGER NOT NULL REFERENCES fields(id),
                    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
                    comment TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);

        } catch (SQLException e) {
            throw new RuntimeException("Không thể khởi tạo cơ sở dữ liệu", e);
        }
    }
}
