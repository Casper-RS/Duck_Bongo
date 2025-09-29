package dev.casperrs.duckbongo.dataHandler;

import java.nio.file.*;
import java.sql.*;

public class DatabaseConnection {
    private static final String DB_DIR  = System.getProperty("user.home") + "/.duckbongo";
    private static final String DB_PATH = DB_DIR + "/duckbongo.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    public static Connection getConnection() throws SQLException {
        try {
            Files.createDirectories(Path.of(DB_DIR));
        } catch (Exception ignored) {}
        return DriverManager.getConnection(JDBC_URL);
    }

    public static void init() throws SQLException {
        try (Connection c = getConnection();
             Statement st = c.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS UserData (
                  UserID     TEXT PRIMARY KEY,
                  Username   TEXT UNIQUE NOT NULL,
                  ClickCount INTEGER NOT NULL DEFAULT 0
                )
            """);

            // Migration: add skin columns if they don't exist yet
            try (Statement alter = c.createStatement()) {
                alter.executeUpdate("ALTER TABLE UserData ADD COLUMN DuckSkin TEXT DEFAULT '/assets/skin_parts/ducks/duck_default.png'");
            } catch (SQLException ignored) { /* column may already exist */ }
            try (Statement alter = c.createStatement()) {
                alter.executeUpdate("ALTER TABLE UserData ADD COLUMN WaterSkin TEXT DEFAULT '/assets/skin_parts/waters/water_default.png'");
            } catch (SQLException ignored) { /* column may already exist */ }

            // Migration: add preference columns (1=true, 0=false)
            try (Statement alter = c.createStatement()) {
                alter.executeUpdate("ALTER TABLE UserData ADD COLUMN ShowNames INTEGER NOT NULL DEFAULT 1");
            } catch (SQLException ignored) { /* column may already exist */ }
            try (Statement alter = c.createStatement()) {
                alter.executeUpdate("ALTER TABLE UserData ADD COLUMN MovementSync INTEGER NOT NULL DEFAULT 1");
            } catch (SQLException ignored) { /* column may already exist */ }

            // Migration: track unlocked cosmetics
            try (Statement alter = c.createStatement()) {
                alter.executeUpdate("ALTER TABLE UserData ADD COLUMN UnlockedCosmetics TEXT DEFAULT ''");
            } catch (SQLException ignored) { /* column may already exist */ }
        }
    }
}

