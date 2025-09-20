package dev.casperrs.duckbongo.input;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.*;
import com.github.kwhat.jnativehook.mouse.*;
import dev.casperrs.duckbongo.ActivityUpdater;
import dev.casperrs.duckbongo.core.PointsManager;

import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

public class InputHook implements NativeKeyListener, NativeMouseInputListener {
    private final PointsManager points;
    private final ActivityUpdater activityUpdater;

    public InputHook(PointsManager points, ActivityUpdater activityUpdater) {
        this.points = points;
        this.activityUpdater = activityUpdater;
    }

    public void start() throws NativeHookException {
        // Dempen van JNativeHook logging
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(this);
        GlobalScreen.addNativeMouseListener(this);
        GlobalScreen.addNativeMouseMotionListener(this);
    }
    public void stop() throws NativeHookException {
        GlobalScreen.unregisterNativeHook();
    }

    private Set<Integer> pressedKeys =  new HashSet<Integer>();

    @Override public void nativeKeyPressed(NativeKeyEvent e) {
        if (!pressedKeys.contains(e.getKeyCode())) {
            points.add(1);

            if (activityUpdater != null) {
                activityUpdater.update("Points: " + points.get(), "Pressing keys!");
            }

            pressedKeys.add(e.getKeyCode());
        }
    }
    @Override public void nativeKeyReleased(NativeKeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }
    @Override public void nativeMousePressed(NativeMouseEvent e) {
        points.add(1);

        if (activityUpdater != null) {
            activityUpdater.update("Points: " + points.get(), "Clicking!");
        }
    }
    @Override public void nativeMouseDragged(NativeMouseEvent e) { /* eventueel throttle */ }
    @Override public void nativeMouseMoved(NativeMouseEvent e) { /* ignore */ }
}

