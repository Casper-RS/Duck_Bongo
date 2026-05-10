package dev.casperrs.duckbongo.data;

import dev.casperrs.duckbongo.core.PointsManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataHandler {

    private static final Logger LOG = Logger.getLogger(DataHandler.class.getName());
    private static final String DEFAULT_USER_ID = "default_user";

    private final PointsManager pointsManager;

    public DataHandler(PointsManager pointsManager) {
        this.pointsManager = pointsManager;
    }

    public void initAndLoad() {
        try {
            DatabaseConnection.init();
            loadPoints();
            LOG.info("DataHandler initialized and points loaded");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to initialize DataHandler", e);
        }
    }

    public void save() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String sql = """
                INSERT INTO UserData (UserID, Username, ClickCount)
                VALUES (?, ?, ?)
                ON CONFLICT(UserID) DO UPDATE SET ClickCount = excluded.ClickCount
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, DEFAULT_USER_ID);
                stmt.setString(2, "default");
                stmt.setLong(3, pointsManager.get());
                stmt.executeUpdate();
            }
        }
    }

    private void loadPoints() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String sql = "SELECT ClickCount FROM UserData WHERE UserID = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, DEFAULT_USER_ID);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long savedPoints = rs.getLong("ClickCount");
                        pointsManager.add(savedPoints);
                        LOG.info("Loaded " + savedPoints + " points from database");
                    } else {
                        LOG.info("No saved points found, starting fresh");
                    }
                }
            }
        }
    }
}