package dev.casperrs.duckbongo.app;

import com.github.kwhat.jnativehook.NativeHookException;
import dev.casperrs.duckbongo.core.PointsManager;
import dev.casperrs.duckbongo.data.DataHandler;
import dev.casperrs.duckbongo.input.InputHook;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    private DuckOverlay overlay;
    private long lastPointsSeen = 0;

    private final PointsManager points = new PointsManager();
    private DataHandler dataHandler = new DataHandler(points);

    @Override
    public void start(Stage stage) {
        stage.setTitle("Duck Bongo");

        dataHandler.initAndLoad();

        overlay = new DuckOverlay(stage, points);
        overlay.show();

        try {
            InputHook hook = new InputHook(points);
            hook.start();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                long currentPoints = points.get();
                if (currentPoints != lastPointsSeen) {
                    overlay.punch();
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
