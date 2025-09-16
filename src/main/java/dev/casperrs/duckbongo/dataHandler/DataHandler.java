package dev.casperrs.duckbongo.dataHandler;

import dev.casperrs.duckbongo.core.PointsManager;
import javafx.scene.control.TextInputDialog;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class DataHandler {
    private static final String FILE_NAME = System.getProperty("user.home") + File.separator + ".duckbongo" + File.separator + "duck_data.properties";
    private static final String USER_ID = "UUID";
    private static final String KEY_CLICKS = "clicks";

    private final Properties props = new Properties();
    private final FetchUserData data = new FetchUserData();

    private String currentUserId;
    private FetchUserData.UserRecord currentUser;

    private final PointsManager points;

    public DataHandler(PointsManager points) {
        this.points = points;
    }

    /** Call bij app-start */
    public void initAndLoad() {
        try {
            DatabaseConnection.init();
            loadProps();

            currentUserId = props.getProperty(USER_ID);
            if (currentUserId == null || currentUserId.isBlank()) {
                // Eerste run: maak userId + vraag username + create row
                currentUserId = UUID.randomUUID().toString();
                String username = promptUniqueUsername();
                currentUser = data.create(currentUserId, username);
                props.setProperty(USER_ID, currentUserId);
                storeProps();
            } else {
                // Bestaande run: haal user op uit DB
                Optional<FetchUserData.UserRecord> rec = data.findById(currentUserId);
                if (rec.isEmpty()) {
                    // DB mist de rij voor deze userId → opnieuw onboarden met zelfde userId
                    String username = promptUniqueUsername();
                    currentUser = data.create(currentUserId, username);
                } else {
                    currentUser = rec.get();
                }
            }

            // MIGRATIE (éénmalig): had je vroeger clicks in properties? voeg toe en meteen saven.
            long oldClicks = parseLong(props.getProperty(KEY_CLICKS, "0"));
            if (oldClicks > 0) {
                points.add(oldClicks);
                // Direct persist naar DB en verwijder oude key uit properties
                save();
                props.remove(KEY_CLICKS);
                storeProps();
            }

            // Laad clickCount uit DB in PointsManager
            points.add(currentUser.clickCount());
            System.out.println("Loaded clicks for " + currentUser.username() + ": " + currentUser.clickCount());

        } catch (SQLException e) {
            throw new RuntimeException("Database init/load failed", e);
        }
    }

    /** Call bij app-exit of periodiek autosave */
    public void save() {
        if (currentUserId == null) return;
        try {
            long current = points.get();
            data.updateClickCount(currentUserId, current);
            System.out.println("Saved clicks (" + current + ") for " + currentUser.username());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===== Helpers =====

    private void loadProps() {
        try {
            Path p = Path.of(FILE_NAME);
            if (Files.exists(p)) {
                try (FileInputStream fis = new FileInputStream(FILE_NAME)) {
                    props.load(fis);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read properties; starting with defaults.");
        }
    }

    private void storeProps() {
        try (FileOutputStream fos = new FileOutputStream(FILE_NAME)) {
            props.store(fos, "DuckBongo local settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Vraag om unieke username via JavaFX (pas aan naar eigen UI indien nodig)
    private String promptUniqueUsername() throws SQLException {
        while (true) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("DuckBongo – Welcome!");
            dialog.setHeaderText("Please pick a username.");
            dialog.setContentText("Username:");

            String name = dialog.showAndWait().map(String::trim).orElse("");
            if (name.isEmpty()) continue;

            if (data.findByUsername(name).isPresent()) {
                // bestaat al → opnieuw vragen
                dialog.setHeaderText("Username is already in use. Please pick another name.");
                continue;
            }
            return name;
        }
    }
    private boolean findByUsernameFree(String name) throws SQLException {
        return data.findByUsername(name).isEmpty();
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0L; }
    }
}