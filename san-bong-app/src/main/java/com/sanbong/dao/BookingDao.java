package com.sanbong.dao;

import com.sanbong.model.Booking;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All write methods accept a {@link Connection} supplied by the caller so
 * {@code BookingService} can wrap the duplicate-check + insert in a single
 * JDBC transaction and avoid race conditions between concurrent bookings.
 */
public class BookingDao {

    private static final String SELECT_JOINED = """
            SELECT b.*, f.name AS field_name, v.name AS venue_name,
                   u.full_name AS customer_name,
                   ts.start_time AS slot_start, ts.end_time AS slot_end
            FROM bookings b
            JOIN fields f ON b.field_id = f.id
            JOIN venues v ON f.venue_id = v.id
            JOIN users u ON b.user_id = u.id
            JOIN time_slots ts ON b.time_slot_id = ts.id
            """;

    public boolean existsActiveBooking(Connection conn, int fieldId, int timeSlotId, LocalDate date) throws SQLException {
        String sql = """
                SELECT 1 FROM bookings
                WHERE field_id = ? AND time_slot_id = ? AND booking_date = ? AND status != 'cancelled'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fieldId);
            ps.setInt(2, timeSlotId);
            ps.setString(3, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insert(Connection conn, Booking booking) throws SQLException {
        String sql = """
                INSERT INTO bookings (user_id, field_id, time_slot_id, booking_date, total_price, status, payment_status, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, booking.getUserId());
            ps.setInt(2, booking.getFieldId());
            ps.setInt(3, booking.getTimeSlotId());
            ps.setString(4, booking.getBookingDate().toString());
            ps.setBigDecimal(5, booking.getTotalPrice());
            ps.setString(6, booking.getStatus());
            ps.setString(7, booking.getPaymentStatus());
            ps.setString(8, booking.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public List<Booking> findByUserId(int userId) {
        String sql = SELECT_JOINED + " WHERE b.user_id = ? ORDER BY b.booking_date DESC, ts.start_time DESC";
        List<Booking> bookings = new ArrayList<>();
        try (Connection conn = com.sanbong.util.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookings.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return bookings;
    }

    public List<Booking> findAll() {
        return findAll(null);
    }

    /** Lists bookings, optionally scoped to a single venue owner's fields (null = all, used by admin). */
    public List<Booking> findAll(Integer ownerId) {
        String sql = SELECT_JOINED
                + (ownerId != null ? " WHERE v.owner_id = ? " : "")
                + " ORDER BY b.booking_date DESC, ts.start_time DESC";
        List<Booking> bookings = new ArrayList<>();
        try (Connection conn = com.sanbong.util.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (ownerId != null) {
                ps.setInt(1, ownerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookings.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return bookings;
    }

    public Optional<Booking> findById(int id) {
        String sql = SELECT_JOINED + " WHERE b.id = ?";
        try (Connection conn = com.sanbong.util.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public void updateStatus(int bookingId, String status) {
        try (Connection conn = com.sanbong.util.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE bookings SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updatePaymentStatus(int bookingId, String paymentStatus) {
        try (Connection conn = com.sanbong.util.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE bookings SET payment_status = ? WHERE id = ?")) {
            ps.setString(1, paymentStatus);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public BigDecimal sumRevenueForMonth(int year, int month) {
        return sumRevenueForMonth(year, month, null);
    }

    public BigDecimal sumRevenueForMonth(int year, int month, Integer ownerId) {
        String sql = """
                SELECT COALESCE(SUM(b.total_price), 0) AS total
                FROM bookings b
                JOIN fields f ON b.field_id = f.id
                JOIN venues v ON f.venue_id = v.id
                WHERE b.status != 'cancelled'
                  AND strftime('%Y', b.booking_date) = ?
                  AND strftime('%m', b.booking_date) = ?
                """ + (ownerId != null ? " AND v.owner_id = ? " : "");
        try (Connection conn = com.sanbong.util.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(year));
            ps.setString(2, String.format("%02d", month));
            if (ownerId != null) {
                ps.setInt(3, ownerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return BigDecimal.ZERO;
    }

    /** Revenue per day-of-month (1-31) for the current month, used by the dashboard trend chart. */
    public java.util.Map<Integer, BigDecimal> revenueByDayForMonth(int year, int month) {
        return revenueByDayForMonth(year, month, null);
    }

    public java.util.Map<Integer, BigDecimal> revenueByDayForMonth(int year, int month, Integer ownerId) {
        String sql = """
                SELECT CAST(strftime('%d', b.booking_date) AS INTEGER) AS day, SUM(b.total_price) AS total
                FROM bookings b
                JOIN fields f ON b.field_id = f.id
                JOIN venues v ON f.venue_id = v.id
                WHERE b.status != 'cancelled'
                  AND strftime('%Y', b.booking_date) = ?
                  AND strftime('%m', b.booking_date) = ?
                """ + (ownerId != null ? " AND v.owner_id = ? " : "") + """
                GROUP BY day
                ORDER BY day
                """;
        java.util.Map<Integer, BigDecimal> result = new java.util.LinkedHashMap<>();
        try (Connection conn = com.sanbong.util.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(year));
            ps.setString(2, String.format("%02d", month));
            if (ownerId != null) {
                ps.setInt(3, ownerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("day"), rs.getBigDecimal("total"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /** Auto-transitions confirmed bookings whose time slot has already ended to 'completed'. */
    public void completePastConfirmedBookings() {
        String sql = """
                UPDATE bookings
                SET status = 'completed'
                WHERE status = 'confirmed'
                  AND (
                    booking_date < ?
                    OR (booking_date = ? AND id IN (
                        SELECT b.id FROM bookings b
                        JOIN time_slots ts ON b.time_slot_id = ts.id
                        WHERE b.booking_date = ? AND ts.end_time <= ?
                    ))
                  )
                """;
        LocalDate today = LocalDate.now();
        String nowTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        try (Connection conn = com.sanbong.util.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, today.toString());
            ps.setString(2, today.toString());
            ps.setString(3, today.toString());
            ps.setString(4, nowTime);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int countBookingsOnDate(LocalDate date) {
        return countBookingsOnDate(date, null);
    }

    public int countBookingsOnDate(LocalDate date, Integer ownerId) {
        String sql = """
                SELECT COUNT(*) AS cnt
                FROM bookings b
                JOIN fields f ON b.field_id = f.id
                JOIN venues v ON f.venue_id = v.id
                WHERE b.booking_date = ? AND b.status != 'cancelled'
                """ + (ownerId != null ? " AND v.owner_id = ? " : "");
        try (Connection conn = com.sanbong.util.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            if (ownerId != null) {
                ps.setInt(2, ownerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private Booking map(ResultSet rs) throws SQLException {
        Booking booking = new Booking();
        booking.setId(rs.getInt("id"));
        booking.setUserId(rs.getInt("user_id"));
        booking.setFieldId(rs.getInt("field_id"));
        booking.setTimeSlotId(rs.getInt("time_slot_id"));
        booking.setBookingDate(LocalDate.parse(rs.getString("booking_date")));
        booking.setTotalPrice(rs.getBigDecimal("total_price"));
        booking.setStatus(rs.getString("status"));
        booking.setPaymentStatus(rs.getString("payment_status"));
        booking.setNote(rs.getString("note"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            booking.setCreatedAt(LocalDateTime.parse(createdAt.replace(' ', 'T')));
        }
        booking.setFieldName(rs.getString("field_name"));
        booking.setVenueName(rs.getString("venue_name"));
        booking.setCustomerName(rs.getString("customer_name"));
        booking.setSlotLabel(rs.getString("slot_start") + " - " + rs.getString("slot_end"));
        return booking;
    }
}
