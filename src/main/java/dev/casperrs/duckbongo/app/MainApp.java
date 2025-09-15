// dev/casperrs/duckbongo/app/MainApp.java
package dev.casperrs.duckbongo.app;

import com.github.kwhat.jnativehook.NativeHookException;
import dev.casperrs.duckbongo.core.PointsManager;
import dev.casperrs.duckbongo.input.InputHook;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    private final PointsManager points = new PointsManager();
    private DuckOverlay overlay;
    private long lastPointsSeen = 0;

    @Override
    public void start(Stage stage) {
        overlay = new DuckOverlay(stage, points);
        overlay.show();

        // Global input hooks
        InputHook hook = new InputHook(points);
        try { hook.start(); } catch (NativeHookException e) { e.printStackTrace(); }

        // Update only when points change (for punch + counter text)
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                long p = points.get();
                if (p != lastPointsSeen) {
                    overlay.punch();          // also updates the counter text
                    lastPointsSeen = p;
                }
            }
        }.start();

    }

    public static void main(String[] args) { launch(args); }
}
