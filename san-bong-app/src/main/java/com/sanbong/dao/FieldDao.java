package com.sanbong.dao;

import com.sanbong.model.Field;
import com.sanbong.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FieldDao {

    private static final String SELECT_WITH_VENUE = """
            SELECT f.*, v.name AS venue_name
            FROM fields f JOIN venues v ON f.venue_id = v.id
            """;

    public List<Field> findAll() {
        String sql = SELECT_WITH_VENUE + " ORDER BY f.id";
        List<Field> fields = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                fields.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return fields;
    }

    /** Lists fields, optionally scoped to a single venue owner (null = all fields, used by admin). */
    public List<Field> findAll(Integer ownerId) {
        if (ownerId == null) {
            return findAll();
        }
        String sql = SELECT_WITH_VENUE + " WHERE v.owner_id = ? ORDER BY f.id";
        List<Field> fields = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fields.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return fields;
    }

    public List<Field> findByType(String type) {
        String sql = SELECT_WITH_VENUE + " WHERE f.type = ? ORDER BY f.id";
        List<Field> fields = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fields.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return fields;
    }

    public Optional<Field> findById(int id) {
        String sql = SELECT_WITH_VENUE + " WHERE f.id = ?";
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

    public int insert(Field field) {
        String sql = "INSERT INTO fields (venue_id, name, type, description, status, image) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, field.getVenueId());
            ps.setString(2, field.getName());
            ps.setString(3, field.getType());
            ps.setString(4, field.getDescription());
            ps.setString(5, field.getStatus());
            ps.setString(6, field.getImage());
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

    public void update(Field field) {
        String sql = "UPDATE fields SET venue_id=?, name=?, type=?, description=?, status=?, image=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, field.getVenueId());
            ps.setString(2, field.getName());
            ps.setString(3, field.getType());
            ps.setString(4, field.getDescription());
            ps.setString(5, field.getStatus());
            ps.setString(6, field.getImage());
            ps.setInt(7, field.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM fields WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Field map(ResultSet rs) throws SQLException {
        Field field = new Field();
        field.setId(rs.getInt("id"));
        field.setVenueId(rs.getInt("venue_id"));
        field.setVenueName(rs.getString("venue_name"));
        field.setName(rs.getString("name"));
        field.setType(rs.getString("type"));
        field.setDescription(rs.getString("description"));
        field.setStatus(rs.getString("status"));
        field.setImage(rs.getString("image"));
        return field;
    }
}
