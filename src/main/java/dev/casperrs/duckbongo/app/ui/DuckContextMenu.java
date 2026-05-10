package dev.casperrs.duckbongo.app.ui;

import dev.casperrs.duckbongo.app.skins.SkinSelectionListener;
import dev.casperrs.duckbongo.app.utils.SkinGallery;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class DuckContextMenu {

    public static ContextMenu build(
            Stage stage,
            CounterBar counterBar,
            SkinSelectionListener skinListener) {

        return new ContextMenu(
                skinGalleryItem(stage, skinListener),
                copyCountItem(counterBar.label()),
                toggleAlwaysOnTopItem(stage),
                snapBottomRightItem(stage),
                exitItem(stage)
        );
    }

    private static MenuItem skinGalleryItem(Stage stage, SkinSelectionListener listener) {
        MenuItem item = new MenuItem("Skin Gallery...");
        item.setOnAction(e -> {
            double x = stage.getX() + 200;
            double y = stage.getY() + 100;
            SkinGallery.show(stage, x, y, listener);
        });
        return item;
    }

    private static MenuItem copyCountItem(Label counterLabel) {
        MenuItem item = new MenuItem("Copy count");
        item.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(counterLabel.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });
        return item;
    }

    private static MenuItem toggleAlwaysOnTopItem(Stage stage) {
        MenuItem item = new MenuItem("Toggle always-on-top");
        item.setOnAction(e -> stage.setAlwaysOnTop(!stage.isAlwaysOnTop()));
        return item;
    }

    private static MenuItem snapBottomRightItem(Stage stage) {
        MenuItem item = new MenuItem("Snap bottom-right");
        item.setOnAction(e -> {
            Rectangle2D vb = Screen.getPrimary().getVisualBounds();
            stage.setX(vb.getMaxX() - stage.getWidth() - 20);
            stage.setY(vb.getMaxY() - stage.getHeight() - 18);
        });
        return item;
    }

    private static MenuItem exitItem(Stage stage) {
        MenuItem item = new MenuItem("Exit");
        item.setOnAction(e -> {
            stage.close();
            Platform.exit();
        });
        return item;
    }

    private DuckContextMenu() {}
}
