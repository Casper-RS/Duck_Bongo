package dev.casperrs.duckbongo.dataHandler;

import java.sql.*;
import java.util.Optional;

public class FetchUserData {

    public Optional<UserRecord> findById(String userId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 SELECT UserID, Username, ClickCount
                 FROM UserData WHERE UserID = ?
             """)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        }
    }

    public Optional<UserRecord> findByUsername(String username) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 SELECT UserID, Username, ClickCount
                 FROM UserData WHERE Username = ?
             """)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    public UserRecord create(String userId, String username) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 INSERT INTO UserData(UserID, Username, ClickCount)
                 VALUES(?, ?, 0)
             """)) {
            ps.setString(1, userId);
            ps.setString(2, username);
            ps.executeUpdate();
            return new UserRecord(userId, username, 0L);
        }
    }

    public void updateClickCount(String userId, long clicks) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 UPDATE UserData SET ClickCount = ?
                 WHERE UserID = ?
             """)) {
            ps.setLong(1, clicks);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    private UserRecord map(ResultSet rs) throws SQLException {
        return new UserRecord(
                rs.getString("UserID"),
                rs.getString("Username"),
                rs.getLong("ClickCount")
        );
    }

    // Simpele DTO
    public record UserRecord(String userId, String username, long clickCount) {}
}
