package dev.casperrs.duckbongo.app.controller;

import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public final class WindowDragController {

    private static final double DRAG_THRESHOLD = 8.0;

    public static void install(Stage stage, Scene scene) {
        final double[] press = new double[2];
        final double[] offset = new double[2];

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            press[0]  = e.getScreenX();
            press[1]  = e.getScreenY();
            offset[0] = press[0] - stage.getX();
            offset[1] = press[1] - stage.getY();
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            double dx = Math.abs(e.getScreenX() - press[0]);
            double dy = Math.abs(e.getScreenY() - press[1]);
            if (dx > DRAG_THRESHOLD || dy > DRAG_THRESHOLD) {
                stage.setX(e.getScreenX() - offset[0]);
                stage.setY(e.getScreenY() - offset[1]);
            }
        });
    }

    private WindowDragController() {}
}
