package com.sanbong.dao;

import com.sanbong.model.Review;
import com.sanbong.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReviewDao {

    public int insert(Review review) {
        String sql = "INSERT INTO reviews (user_id, field_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, review.getUserId());
            ps.setInt(2, review.getFieldId());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getComment());
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

    public boolean existsByUserAndField(int userId, int fieldId) {
        String sql = "SELECT 1 FROM reviews WHERE user_id = ? AND field_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, fieldId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Review> findByFieldId(int fieldId) {
        String sql = """
                SELECT r.*, u.full_name AS customer_name
                FROM reviews r JOIN users u ON r.user_id = u.id
                WHERE r.field_id = ? ORDER BY r.created_at DESC
                """;
        List<Review> reviews = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fieldId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return reviews;
    }

    /** Returns {average, count} for the field; average is 0 when there are no reviews. */
    public double[] averageAndCount(int fieldId) {
        String sql = "SELECT AVG(rating) AS avg_rating, COUNT(*) AS cnt FROM reviews WHERE field_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fieldId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new double[]{rs.getDouble("avg_rating"), rs.getInt("cnt")};
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new double[]{0, 0};
    }

    private Review map(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setId(rs.getInt("id"));
        review.setUserId(rs.getInt("user_id"));
        review.setFieldId(rs.getInt("field_id"));
        review.setRating(rs.getInt("rating"));
        review.setComment(rs.getString("comment"));
        review.setCustomerName(rs.getString("customer_name"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            review.setCreatedAt(LocalDateTime.parse(createdAt.replace(' ', 'T')));
        }
        return review;
    }
}
