package dev.casperrs.duckbongo.input;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.*;
import com.github.kwhat.jnativehook.mouse.*;
import dev.casperrs.duckbongo.core.PointsManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class InputHook implements NativeKeyListener, NativeMouseInputListener {
    private final PointsManager points;

    public InputHook(PointsManager points) { this.points = points; }

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

    @Override public void nativeKeyPressed(NativeKeyEvent e) { points.add(1); }
    @Override public void nativeMousePressed(NativeMouseEvent e) { points.add(1); }
    @Override public void nativeMouseDragged(NativeMouseEvent e) { /* eventueel throttle */ }
    @Override public void nativeMouseMoved(NativeMouseEvent e) { /* ignore */ }
}

