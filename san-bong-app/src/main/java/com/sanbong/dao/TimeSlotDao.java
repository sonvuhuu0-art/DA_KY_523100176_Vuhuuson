package com.sanbong.dao;

import com.sanbong.model.TimeSlot;
import com.sanbong.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TimeSlotDao {

    public List<TimeSlot> findByFieldId(int fieldId) {
        String sql = "SELECT * FROM time_slots WHERE field_id = ? ORDER BY day_type, start_time";
        List<TimeSlot> slots = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fieldId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    slots.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return slots;
    }

    public Optional<TimeSlot> findById(int id) {
        String sql = "SELECT * FROM time_slots WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
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

    /** Returns every time slot of the field for the requested day, flagged as booked or free. */
    public List<TimeSlot> findAvailableSlots(int fieldId, LocalDate date) {
        String dayType = (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)
                ? "weekend" : "weekday";
        String sql = """
                SELECT ts.*,
                       EXISTS (
                           SELECT 1 FROM bookings b
                           WHERE b.time_slot_id = ts.id
                             AND b.booking_date = ?
                             AND b.status != 'cancelled'
                       ) AS is_booked
                FROM time_slots ts
                WHERE ts.field_id = ? AND ts.day_type = ?
                ORDER BY ts.start_time
                """;
        List<TimeSlot> slots = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setInt(2, fieldId);
            ps.setString(3, dayType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TimeSlot slot = map(rs);
                    slot.setBooked(rs.getInt("is_booked") == 1);
                    slots.add(slot);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return slots;
    }

    /** Counts time slots of the given day type belonging to currently active fields — used for occupancy stats. */
    public int countActiveSlotsForDayType(String dayType) {
        return countActiveSlotsForDayType(dayType, null);
    }

    public int countActiveSlotsForDayType(String dayType, Integer ownerId) {
        String sql = """
                SELECT COUNT(*) AS cnt FROM time_slots ts
                JOIN fields f ON ts.field_id = f.id
                JOIN venues v ON f.venue_id = v.id
                WHERE f.status = 'active' AND ts.day_type = ?
                """ + (ownerId != null ? " AND v.owner_id = ? " : "");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dayType);
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

    public int insert(TimeSlot slot) {
        String sql = "INSERT INTO time_slots (field_id, start_time, end_time, day_type, price) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, slot.getFieldId());
            ps.setString(2, slot.getStartTime().toString());
            ps.setString(3, slot.getEndTime().toString());
            ps.setString(4, slot.getDayType());
            ps.setBigDecimal(5, slot.getPrice());
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

    public void update(TimeSlot slot) {
        String sql = "UPDATE time_slots SET start_time=?, end_time=?, day_type=?, price=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slot.getStartTime().toString());
            ps.setString(2, slot.getEndTime().toString());
            ps.setString(3, slot.getDayType());
            ps.setBigDecimal(4, slot.getPrice());
            ps.setInt(5, slot.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM time_slots WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private TimeSlot map(ResultSet rs) throws SQLException {
        TimeSlot slot = new TimeSlot();
        slot.setId(rs.getInt("id"));
        slot.setFieldId(rs.getInt("field_id"));
        slot.setStartTime(LocalTime.parse(rs.getString("start_time")));
        slot.setEndTime(LocalTime.parse(rs.getString("end_time")));
        slot.setDayType(rs.getString("day_type"));
        slot.setPrice(rs.getBigDecimal("price"));
        return slot;
    }
}
