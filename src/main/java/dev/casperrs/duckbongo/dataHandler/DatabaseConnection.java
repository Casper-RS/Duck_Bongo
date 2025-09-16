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
        }
    }
}

