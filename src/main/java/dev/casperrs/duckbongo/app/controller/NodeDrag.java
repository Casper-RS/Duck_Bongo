package dev.casperrs.duckbongo.app.controller;

import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

import java.util.function.BiConsumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public final class NodeDrag {

    @FunctionalInterface
    public interface PositionSink extends BiConsumer<Float, Float> {}

    public static void install(
            Node target,
            DoubleSupplier readX, DoubleSupplier readY,
            DoubleConsumer  writeX, DoubleConsumer  writeY,
            Runnable onPress,
            PositionSink onMove,
            PositionSink onRelease) {

        final double[] pressScene = new double[2];
        final double[] startTrans = new double[2];

        target.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            pressScene[0] = e.getSceneX();
            pressScene[1] = e.getSceneY();
            startTrans[0] = readX.getAsDouble();
            startTrans[1] = readY.getAsDouble();
            if (onPress != null) onPress.run();
            e.consume();
        });

        target.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double nx = startTrans[0] + (e.getSceneX() - pressScene[0]);
            double ny = startTrans[1] + (e.getSceneY() - pressScene[1]);
            writeX.accept(nx);
            writeY.accept(ny);
            if (onMove != null) onMove.accept((float) nx, (float) ny);
            e.consume();
        });

        target.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            float fx = (float) readX.getAsDouble();
            float fy = (float) readY.getAsDouble();
            if (onRelease != null) onRelease.accept(fx, fy);
            e.consume();
        });
    }

    private NodeDrag() {}
}
