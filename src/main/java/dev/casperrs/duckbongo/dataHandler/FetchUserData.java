package dev.casperrs.duckbongo.dataHandler;

import java.sql.*;
import java.util.Optional;

public class FetchUserData {

    public Optional<UserRecord> findById(String userId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                SELECT UserID, Username, ClickCount, DuckSkin, WaterSkin, ShowNames, MovementSync, UnlockedCosmetics
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
                SELECT UserID, Username, ClickCount, DuckSkin, WaterSkin, ShowNames, MovementSync, UnlockedCosmetics
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
                INSERT INTO UserData(UserID, Username, ClickCount, DuckSkin, WaterSkin, ShowNames, MovementSync, UnlockedCosmetics)
                VALUES(?, ?, 0, '/assets/skin_parts/ducks/duck_default.png', '/assets/skin_parts/waters/water_default.png', 1, 1, '')
            """)) {
            ps.setString(1, userId);
            ps.setString(2, username);
            ps.executeUpdate();
            return new UserRecord(userId, username, 0L,
                    "/assets/skin_parts/ducks/duck_default.png",
                    "/assets/skin_parts/waters/water_default.png",
                    true,
                    true,
                    "");
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

    public void updateSkins(String userId, String duckSkin, String waterSkin) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                UPDATE UserData SET DuckSkin = ?, WaterSkin = ?
                WHERE UserID = ?
            """)) {
            ps.setString(1, duckSkin);
            ps.setString(2, waterSkin);
            ps.setString(3, userId);
            ps.executeUpdate();
        }
    }

    public void updatePreferences(String userId, boolean showNames, boolean movementSync) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                UPDATE UserData SET ShowNames = ?, MovementSync = ?
                WHERE UserID = ?
            """)) {
            ps.setInt(1, showNames ? 1 : 0);
            ps.setInt(2, movementSync ? 1 : 0);
            ps.setString(3, userId);
            ps.executeUpdate();
        }
    }

    public void updateUnlockedCosmetics(String userId, String unlockedCsv) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                UPDATE UserData SET UnlockedCosmetics = ?
                WHERE UserID = ?
            """)) {
            ps.setString(1, unlockedCsv);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    private UserRecord map(ResultSet rs) throws SQLException {
        return new UserRecord(
                rs.getString("UserID"),
                rs.getString("Username"),
                rs.getLong("ClickCount"),
                rs.getString("DuckSkin"),
                rs.getString("WaterSkin"),
                rs.getInt("ShowNames") != 0,
                rs.getInt("MovementSync") != 0,
                rs.getString("UnlockedCosmetics")
        );
    }

    // Simpele DTO
    public record UserRecord(String userId, String username, long clickCount,
                             String duckSkin, String waterSkin,
                             boolean showNames, boolean movementSync,
                             String unlockedCosmetics) {}
}
