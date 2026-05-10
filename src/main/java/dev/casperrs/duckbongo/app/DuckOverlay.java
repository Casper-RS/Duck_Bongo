package dev.casperrs.duckbongo.app;

import dev.casperrs.duckbongo.app.controller.NodeDrag;
import dev.casperrs.duckbongo.app.controller.WindowDragController;
import dev.casperrs.duckbongo.app.skins.SkinSelectionListener;
import dev.casperrs.duckbongo.app.skins.SkinSet;
import dev.casperrs.duckbongo.app.ui.CounterBar;
import dev.casperrs.duckbongo.app.ui.DuckContextMenu;
import dev.casperrs.duckbongo.app.ui.HamburgerButton;
import dev.casperrs.duckbongo.app.utils.DuckView;
import dev.casperrs.duckbongo.core.PointsManager;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public final class DuckOverlay {

    private static final int DUCK_WIDTH     = 140;
    private static final int BAR_WIDTH      = 100;
    private static final int BAR_HEIGHT     = 22;
    private static final int MENU_ICON      = 22;
    private static final int BAR_OVERLAP_PX = 22;

    private final Stage stage;
    private final PointsManager points;
    private final DuckView localDuck = new DuckView(DUCK_WIDTH, true);
    private final VBox column;
    private final CounterBar counterBar;

    private SkinSet skins = SkinSet.DEFAULT;

    public DuckOverlay(Stage stage, PointsManager points) {
        this.stage = stage;
        this.points = points;

        localDuck.setSkins(skins);
        this.counterBar = new CounterBar(BAR_WIDTH, BAR_HEIGHT, points.get());

        HamburgerButton hamburger = new HamburgerButton(MENU_ICON, this::contextMenu);
        StackPane localStack = new StackPane(localDuck.node());
        localStack.setAlignment(Pos.CENTER_LEFT);
        HBox barRow = new HBox(6, counterBar.node(), hamburger.node());
        barRow.setAlignment(Pos.CENTER);
        barRow.setTranslateY(-BAR_OVERLAP_PX);

        this.column = new VBox(0, localStack, barRow);
        column.setAlignment(Pos.TOP_LEFT);
        column.setPadding(new Insets(2, 4, 4, 4));

        Group root = new Group(column);

        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, vb.getWidth(), vb.getHeight());
        scene.setFill(null);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);
        stage.setX(vb.getMinX());
        stage.setY(vb.getMinY());

        WindowDragController.install(stage, scene);
        installLocalDuckDrag();

        column.setTranslateX(10);
        column.setTranslateY(10);
        stage.show();
    }

    public void changeDuckSkin(String path) {
        Platform.runLater(() -> {
            skins = skins.withDuck(path);
            localDuck.setSkins(skins);
            column.toFront();
        });
    }

    public void changeWaterSkin(String path) {
        Platform.runLater(() -> {
            skins = skins.withWater(path);
            localDuck.setSkins(skins);
            column.toFront();
        });
    }

    public void punch() {
        ScaleTransition st = new ScaleTransition(Duration.millis(80), localDuck.node());
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(0.92);  st.setToY(0.92);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    public void refreshCounter() { counterBar.setValue(points.get()); }

    private ContextMenu contextMenu() {
        return DuckContextMenu.build(
                stage,
                counterBar,
                new SkinSelectionListener() {
                    @Override public void onDuckPicked(String p)  { changeDuckSkin(p); }
                    @Override public void onWaterPicked(String p) { changeWaterSkin(p); }
                }
        );
    }

    private void installLocalDuckDrag() {
        NodeDrag.install(
                localDuck.node(),
                column::getTranslateX, column::getTranslateY,
                column::setTranslateX, column::setTranslateY,
                column::toFront,
                null,
                null
        );
    }
}
