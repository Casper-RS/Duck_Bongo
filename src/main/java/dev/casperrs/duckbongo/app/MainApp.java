package dev.casperrs.duckbongo.app;

import com.github.kwhat.jnativehook.NativeHookException;
import de.jcm.discordgamesdk.activity.Activity;
import dev.casperrs.duckbongo.ActivityExample;
import dev.casperrs.duckbongo.ActivityUpdater;
import dev.casperrs.duckbongo.core.PointsManager;
import dev.casperrs.duckbongo.dataHandler.DataHandler;
import dev.casperrs.duckbongo.input.InputHook;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.stage.Stage;

import java.time.Instant;

public class MainApp extends Application {
    private DuckOverlay overlay;
    private long lastPointsSeen = 0;

    private final PointsManager points = new PointsManager();
    private DataHandler dataHandler = new DataHandler(points);

    @Override
    public void start(Stage stage) {
        stage.setTitle("Duck Bongo");

        // Load saved points
        dataHandler.initAndLoad();

        // Setup overlay
        overlay = new DuckOverlay(stage, points);
        overlay.show();

        // ActivityUpdater: updates Discord activity
        ActivityUpdater updater = (details, state) -> {
            ActivityExample.updateActivity(details, state);
        };

        // Start input hooks
        try {
            InputHook hook = new InputHook(points, updater);
            hook.start();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }

        // Start Discord RPC in a separate thread (creates core + initial activity)
        new Thread(() -> {
            try {
                ActivityExample.runActivityHook(points);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Discord-RPC").start();

        // Update overlay only when points change
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                long currentPoints = points.get();
                if (currentPoints != lastPointsSeen) {
                    overlay.punch(); // animates duck + updates counter
                    lastPointsSeen = currentPoints;
                }
            }
        }.start();
    }

    @Override
    public void stop() {
        if (dataHandler != null) {
            dataHandler.save();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
