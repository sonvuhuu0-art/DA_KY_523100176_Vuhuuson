package com.sanbong.dao;

import com.sanbong.model.Venue;
import com.sanbong.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VenueDao {

    private static final String SELECT_WITH_OWNER = """
            SELECT v.*, u.full_name AS owner_name
            FROM venues v LEFT JOIN users u ON v.owner_id = u.id
            """;

    public List<Venue> findAll() {
        return findAll(null);
    }

    /** Lists venues, optionally scoped to a single owner (null = all venues, used by admin). */
    public List<Venue> findAll(Integer ownerId) {
        String sql = SELECT_WITH_OWNER + (ownerId != null ? " WHERE v.owner_id = ? " : "") + " ORDER BY v.id";
        List<Venue> venues = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (ownerId != null) {
                ps.setInt(1, ownerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    venues.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return venues;
    }

    public Optional<Venue> findById(int id) {
        String sql = SELECT_WITH_OWNER + " WHERE v.id = ?";
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

    public int insert(Venue venue) {
        String sql = "INSERT INTO venues (owner_id, name, address, phone, description, image, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, venue.getOwnerId());
            ps.setString(2, venue.getName());
            ps.setString(3, venue.getAddress());
            ps.setString(4, venue.getPhone());
            ps.setString(5, venue.getDescription());
            ps.setString(6, venue.getImage());
            ps.setString(7, venue.getStatus());
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

    public void update(Venue venue) {
        String sql = "UPDATE venues SET owner_id=?, name=?, address=?, phone=?, description=?, image=?, status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, venue.getOwnerId());
            ps.setString(2, venue.getName());
            ps.setString(3, venue.getAddress());
            ps.setString(4, venue.getPhone());
            ps.setString(5, venue.getDescription());
            ps.setString(6, venue.getImage());
            ps.setString(7, venue.getStatus());
            ps.setInt(8, venue.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM venues WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Venue map(ResultSet rs) throws SQLException {
        Venue venue = new Venue();
        venue.setId(rs.getInt("id"));
        venue.setOwnerId(rs.getInt("owner_id"));
        venue.setOwnerName(rs.getString("owner_name"));
        venue.setName(rs.getString("name"));
        venue.setAddress(rs.getString("address"));
        venue.setPhone(rs.getString("phone"));
        venue.setDescription(rs.getString("description"));
        venue.setImage(rs.getString("image"));
        venue.setStatus(rs.getString("status"));
        return venue;
    }
}
