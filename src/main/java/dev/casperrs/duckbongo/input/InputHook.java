package dev.casperrs.duckbongo.input;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import dev.casperrs.duckbongo.core.PointsManager;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InputHook implements NativeKeyListener, NativeMouseInputListener {
    private final PointsManager points;
    private final Set<Integer> pressedKeys = new HashSet<>();

    public InputHook(PointsManager points) {
        this.points = points;
    }

    public void start() throws NativeHookException {
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

    @Override public void nativeKeyPressed(NativeKeyEvent e) {
        if (pressedKeys.add(e.getKeyCode())) {
            points.add(1);
        }
    }

    @Override public void nativeKeyReleased(NativeKeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    @Override public void nativeMousePressed(NativeMouseEvent e) {
        points.add(1);
    }

    @Override public void nativeMouseDragged(NativeMouseEvent e) { /* ignore */ }
    @Override public void nativeMouseMoved(NativeMouseEvent e)   { /* ignore */ }
}
