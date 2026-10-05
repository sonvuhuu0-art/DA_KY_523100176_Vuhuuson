package com.sanbong.dao;

import com.sanbong.model.Payment;
import com.sanbong.util.DatabaseConnection;

import java.sql.*;

public class PaymentDao {

    public int insert(Payment payment) {
        String sql = """
                INSERT INTO payments (booking_id, amount, method, status, paid_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, payment.getBookingId());
            ps.setBigDecimal(2, payment.getAmount());
            ps.setString(3, payment.getMethod());
            ps.setString(4, payment.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }
}
