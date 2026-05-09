package dev.casperrs.duckbongo.app;

import dev.casperrs.duckbongo.core.PointsManager;
import dev.casperrs.duckbongo.data.DataHandler;
import dev.casperrs.duckbongo.input.InputHook;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApp extends Application {

    private static final Logger LOG = Logger.getLogger(MainApp.class.getName());

    private static final long AUTOSAVE_INTERVAL_S = 30;

    private final PointsManager points      = new PointsManager();
    private final DataHandler   dataHandler = new DataHandler(points);
    private final AtomicBoolean shutdownDone = new AtomicBoolean(false);

    private DuckOverlay overlay;
    private InputHook   inputHook;
    private ScheduledExecutorService autosave;
    private long        lastPointsSeen = 0;

    @Override
    public void start(Stage stage) {
        overlay = new DuckOverlay(stage, points);
        dataHandler.initAndLoad();
        startInputHook();
        startAutosave();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "DuckBongo-Shutdown"));

        new AnimationTimer() {
            @Override public void handle(long now) {
                long current = points.get();
                if (current != lastPointsSeen) {
                    overlay.refreshCounter();
                    overlay.punch();
                    lastPointsSeen = current;
                }
            }
        }.start();
    }

    @Override
    public void stop() { shutdown(); }

    public static void main(String[] args) { launch(args); }

    private void startInputHook() {
        try {
            inputHook = new InputHook(points);
            inputHook.start();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to start input hook", e);
        }
    }

    private void startAutosave() {
        autosave = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DuckBongo-Autosave");
            t.setDaemon(true);
            return t;
        });
        autosave.scheduleAtFixedRate(this::saveSafely,
                AUTOSAVE_INTERVAL_S, AUTOSAVE_INTERVAL_S, TimeUnit.SECONDS);
    }

    private void shutdown() {
        if (!shutdownDone.compareAndSet(false, true)) return;
        if (autosave != null) autosave.shutdownNow();
        saveSafely();
        if (inputHook != null) {
            try { inputHook.stop(); } catch (Exception e) {
                LOG.log(Level.FINE, "Input hook stop failed", e);
            }
        }
        LOG.info("DuckBongo shutting down...");
    }

    private void saveSafely() {
        try {
            dataHandler.save();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Save failed", e);
        }
    }
}
