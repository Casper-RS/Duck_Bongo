package dev.casperrs.duckbongo.data;

import dev.casperrs.duckbongo.core.PointsManager;
import javafx.scene.control.TextInputDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DataHandler {

    private static final Logger LOG = Logger.getLogger(DataHandler.class.getName());

    private static final String FILE_NAME =
            System.getProperty("user.home") + File.separator + ".duckbongo" + File.separator + "duck_data.properties";
    private static final String USER_ID    = "UUID";
    private static final String KEY_CLICKS = "clicks";

    private final Properties props = new Properties();
    private final FetchUserData data = new FetchUserData();
    private final PointsManager points;

    private String currentUserId;
    private FetchUserData.UserRecord currentUser;

    public DataHandler(PointsManager points) {
        this.points = points;
    }

    /** Load (or create) the user record at app startup and seed the in-memory click count. */
    public void initAndLoad() {
        try {
            DatabaseConnection.init();
            loadProps();

            currentUserId = props.getProperty(USER_ID);
            if (currentUserId == null || currentUserId.isBlank()) {
                currentUserId = UUID.randomUUID().toString();
                String username = promptUniqueUsername();
                currentUser = data.create(currentUserId, username);
                props.setProperty(USER_ID, currentUserId);
                storeProps();
            } else {
                Optional<FetchUserData.UserRecord> rec = data.findById(currentUserId);
                if (rec.isEmpty()) {
                    String username = promptUniqueUsername();
                    currentUser = data.create(currentUserId, username);
                } else {
                    currentUser = rec.get();
                }
            }

            // One-shot migration from the legacy properties-based click count.
            long oldClicks = parseLong(props.getProperty(KEY_CLICKS, "0"));
            if (oldClicks > 0) {
                points.add(oldClicks);
                save();
                props.remove(KEY_CLICKS);
                storeProps();
            }

            points.add(currentUser.clickCount());
            LOG.info(() -> "Loaded clicks for " + currentUser.username() + ": " + currentUser.clickCount());

        } catch (SQLException e) {
            throw new IllegalStateException("Database init/load failed", e);
        }
    }

    /** Persist the current click count. Safe to call from multiple threads. */
    public synchronized void save() {
        if (currentUserId == null) return;
        try {
            long current = points.get();
            data.updateClickCount(currentUserId, current);
            LOG.info(() -> "Saved clicks (" + current + ") for " + currentUser.username());
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to save click count", e);
        }
    }

    private void loadProps() {
        Path p = Path.of(FILE_NAME);
        if (!Files.exists(p)) return;
        try (FileInputStream fis = new FileInputStream(FILE_NAME)) {
            props.load(fis);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not read properties; starting with defaults", e);
        }
    }

    private void storeProps() {
        try (FileOutputStream fos = new FileOutputStream(FILE_NAME)) {
            props.store(fos, "DuckBongo local settings");
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to store properties to " + FILE_NAME, e);
        }
    }

    private String promptUniqueUsername() throws SQLException {
        while (true) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("DuckBongo - Welcome!");
            dialog.setHeaderText("Please pick a username.");
            dialog.setContentText("Username:");

            String name = dialog.showAndWait().map(String::trim).orElse("");
            if (name.isEmpty()) continue;

            if (data.findByUsername(name).isPresent()) {
                dialog.setHeaderText("Username is already in use. Please pick another name.");
                continue;
            }
            return name;
        }
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
    }
}
