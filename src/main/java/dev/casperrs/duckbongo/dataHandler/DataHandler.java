package dev.casperrs.duckbongo.dataHandler;

import dev.casperrs.duckbongo.core.PointsManager;

import java.io.*;
import java.util.Properties;

public class DataHandler {
    private static final String FILE_NAME = System.getProperty("user.home") + File.separator + "duck_data.properties";;
    private static final String KEY_CLICKS = "clicks";

    private final PointsManager points;

    public DataHandler(PointsManager points) {
        this.points = points;
    }

    public void load() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(FILE_NAME)) {
            props.load(fis);
            long saved = Long.parseLong(props.getProperty(KEY_CLICKS, "0"));
            points.add(saved);
            System.out.println("Loaded clicks: " + saved);
        } catch (IOException e) {
            System.out.println("No save file found, starting fresh.");
        }
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty(KEY_CLICKS, String.valueOf(points.get()));
        try (FileOutputStream fos = new FileOutputStream(FILE_NAME)) {
            props.store(fos, "Duck Click Data");
            System.out.println("Saved clicks: " + points.get());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}