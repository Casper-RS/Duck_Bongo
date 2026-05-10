package dev.casperrs.duckbongo.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseConnection {

    private static final Logger LOG = Logger.getLogger(DatabaseConnection.class.getName());

    private static final String DB_DIR   = System.getProperty("user.home") + "/.duckbongo";
    private static final String DB_PATH  = DB_DIR + "/duckbongo.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    public static Connection getConnection() throws SQLException {
        try {
            Files.createDirectories(Path.of(DB_DIR));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to create database directory " + DB_DIR, e);
        }
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

    private DatabaseConnection() {}
}
