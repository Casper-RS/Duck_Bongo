package dev.casperrs.duckbongo.app;

import dev.casperrs.duckbongo.app.utils.DuckEvents;
import dev.casperrs.duckbongo.app.utils.DuckView;
import dev.casperrs.duckbongo.app.utils.ResourceUtils;
import dev.casperrs.duckbongo.app.utils.SkinGallery;
import dev.casperrs.duckbongo.core.PointsManager;
import dev.casperrs.duckbongo.network.DuckState;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import static dev.casperrs.duckbongo.app.utils.ResourceUtils.loadFlexible;

public class DuckOverlay {

    // --- UI config
    private static final int DUCK_WIDTH = 140;
    private static final int BAR_WIDTH  = 100;
    private static final int BAR_HEIGHT = 22;
    private static final int MENU_ICON  = 22;

    private final PointsManager points;
    private final Stage stage;
    private final ContextMenu menu;

    // Local + remote ducks
    private final DuckView localDuck = new DuckView(DUCK_WIDTH, true);
    private final Map<Integer, DuckView> otherDucks = new HashMap<>();
    private volatile int myId = -1;

    // Containers
    private Group othersLayer;
    private VBox column;

    // Skins
    private String waterSkin = "/assets/skin_parts/waters/water_default.png";
    private String duckSkin  = "/assets/skin_parts/ducks/duck_default.png";

    // Events
    private DuckEvents events;

    // Dragging state
    private double pressScreenX, pressScreenY, dragOffsetX, dragOffsetY, barPressScreenX, barPressScreenY;
    private boolean isDragging, didDrag;

    public DuckOverlay(Stage stage, PointsManager points) {
        this.stage = stage;
        this.points = points;

        // Compose initial local duck image
        refreshLocalImage();

        // Counter bar
        Label counterText = new Label(format(points.get()));
        counterText.setStyle("-fx-text-fill:#1f2428;-fx-font-size:12px;-fx-font-weight:bold;");
        Region bg = new Region();
        bg.setPrefSize(BAR_WIDTH, BAR_HEIGHT);
        bg.setStyle("""
            -fx-background-color:#bad1e8;
            -fx-background-radius:8;
            -fx-border-color:#3a4147;
            -fx-border-width:1;
            -fx-border-radius:9;
            -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);
        """);
        StackPane counterBar = new StackPane(bg, counterText);
        counterBar.setPadding(new Insets(0));
        StackPane hamburger = buildHamburgerButton();

        // Top stack with duck
        StackPane localStack = new StackPane(localDuck.node()); // bread icon weggelaten voor eenvoud
        localStack.setAlignment(Pos.CENTER_LEFT);

        HBox barRow = new HBox(6, counterBar, hamburger);
        barRow.setAlignment(Pos.CENTER);

        column = new VBox(0, localStack, barRow);
        column.setAlignment(Pos.TOP_LEFT);
        column.setPadding(new Insets(2, 4, 4, 4));

        othersLayer = new Group();
        Group root = new Group(othersLayer, column);

        Scene scene = new Scene(root,
                Math.max(DUCK_WIDTH + 120, BAR_WIDTH + 5 + MENU_ICON) + 20,
                DUCK_WIDTH + BAR_HEIGHT + 60);
        scene.setFill(null);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);

        enableWindowDrag(scene);
        enableToggleOnBar(counterBar);

        stage.setX(20);
        stage.setY(Screen.getPrimary().getVisualBounds().getMaxY() - stage.getHeight() - 18);
        column.setTranslateX(10);
        column.setTranslateY(10);
        stage.show();

        // Local dragging of duck (moves whole column)
        final double[] press = new double[2];
        final double[] start = new double[2];
        localDuck.node().addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            press[0] = e.getSceneX(); press[1] = e.getSceneY();
            start[0] = column.getTranslateX(); start[1] = column.getTranslateY();
            isDragging = true;
            column.toFront();
            e.consume();
        });
        localDuck.node().addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double dx = e.getSceneX() - press[0];
            double dy = e.getSceneY() - press[1];
            column.setTranslateX(start[0] + dx);
            column.setTranslateY(start[1] + dy);
            if (events != null) events.onPositionChanged((float) column.getTranslateX(), (float) column.getTranslateY());
            e.consume();
        });
        localDuck.node().addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (events != null) events.onPositionChanged((float) column.getTranslateX(), (float) column.getTranslateY());
            isDragging = false;
        });

        // Counter punch animation trigger (simple: expose a method you can call externally)
        this.menu = buildContextMenu(counterText);
    }

    // ========== Public API ==========
    public void setEvents(DuckEvents events) { this.events = events; }
    public void setMyId(int id) { this.myId = id; }

    public float getDuckX() { return (float) column.getTranslateX(); }
    public float getDuckY() { return (float) column.getTranslateY(); }
    public String getDuckSkin() { return duckSkin; }
    public String getWaterSkin() { return waterSkin; }

    public void changeDuckSkin(String path) {
        Platform.runLater(() -> {
            duckSkin = ResourceUtils.normDuck(path);
            refreshLocalImage();
            if (events != null) events.onDuckSkinChanged(duckSkin);
        });
    }

    public void changeWaterSkin(String path) {
        Platform.runLater(() -> {
            waterSkin = ResourceUtils.normWater(path);
            refreshLocalImage();
            if (events != null) events.onWaterSkinChanged(waterSkin);
        });
    }

    public void updateWorld(Map<Integer, DuckState> world) {
        Platform.runLater(() -> {
            // Upsert remotes
            for (var e : world.entrySet()) {
                int id = e.getKey();
                if (id == myId) continue;
                DuckState s = e.getValue();

                DuckView dv = otherDucks.get(id);
                if (dv == null) {
                    dv = new DuckView(DUCK_WIDTH, false);
                    dv.rememberPaths(s.skin, s.water);
                    dv.setSkins(loadFlexible(getClass(), ResourceUtils.normDuck(s.skin)),
                            loadFlexible(getClass(), ResourceUtils.normWater(s.water)));
                    otherDucks.put(id, dv);
                    othersLayer.getChildren().add(dv.node());
                    column.toFront();
                } else {
                    if (!Objects.equals(dv.duckPath(), s.skin) || !Objects.equals(dv.waterPath(), s.water)) {
                        dv.rememberPaths(s.skin, s.water);
                        dv.setSkins(loadFlexible(getClass(), ResourceUtils.normDuck(s.skin)),
                                loadFlexible(getClass(), ResourceUtils.normWater(s.water)));
                    }
                }
                dv.setTranslate(s.x, s.y);
            }

            // Remove stale
            otherDucks.keySet().removeIf(id -> {
                if (!world.containsKey(id)) {
                    DuckView dv = otherDucks.get(id);
                    if (dv != null) othersLayer.getChildren().remove(dv.node());
                    return true;
                }
                return false;
            });
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

    // ========== Private helpers ==========
    private void refreshLocalImage() {
        Image duck  = loadFlexible(getClass(), duckSkin);
        Image water = loadFlexible(getClass(), waterSkin);
        localDuck.setSkins(duck, water);
        localDuck.rememberPaths(duckSkin, waterSkin);
        columnToFrontIfReady();
    }

    private void columnToFrontIfReady() {
        if (column != null) column.toFront();
    }

    private ContextMenu buildContextMenu(Label counterText) {
        MenuItem copyCount = new MenuItem("Copy count");
        copyCount.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(counterText.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });

        MenuItem addOne = new MenuItem("Add 1 bread (test)");
        addOne.setOnAction(e -> { points.add(1); counterText.setText(format(points.get())); punch(); });

        MenuItem toggleTop = new MenuItem("Toggle always-on-top");
        toggleTop.setOnAction(e -> stage.setAlwaysOnTop(!stage.isAlwaysOnTop()));

        MenuItem setIp = new MenuItem("Set Server IP...");
        setIp.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Server IP");
            dlg.setHeaderText("Connect to server");
            dlg.setContentText("Enter server IP:");
            dlg.showAndWait().ifPresent(ip -> {
                if (events != null && ip != null && !ip.isBlank()) events.onServerIpSubmit(ip.trim());
            });
        });

        MenuItem skinPopup = new MenuItem("Skin Gallery...");
        skinPopup.setOnAction(e -> {
            double x = stage.getX() + 200, y = stage.getY() + 100;
            SkinGallery.show(stage, x, y, new SkinGallery.OnPick() {
                public void duck(String duckPath)   { changeDuckSkin(duckPath); }
                public void water(String waterPath) { changeWaterSkin(waterPath); }
            });
        });

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> { stage.close(); Platform.exit(); System.exit(0); });

        return new ContextMenu(setIp, skinPopup, addOne, copyCount, toggleTop, exit);
    }

    private void enableWindowDrag(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            pressScreenX = e.getScreenX(); pressScreenY = e.getScreenY();
            dragOffsetX  = pressScreenX - stage.getX(); dragOffsetY = pressScreenY - stage.getY();
            didDrag = false;
        });
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            double dx = Math.abs(e.getScreenX() - pressScreenX);
            double dy = Math.abs(e.getScreenY() - pressScreenY);
            if (dx > 8 || dy > 8) {
                stage.setX(e.getScreenX() - dragOffsetX);
                stage.setY(e.getScreenY() - dragOffsetY);
                didDrag = true;
            }
        });
    }

    private void enableToggleOnBar(javafx.scene.Node bar) {
        bar.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> { barPressScreenX = e.getScreenX(); barPressScreenY = e.getScreenY(); });
        bar.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            double dx = Math.abs(e.getSceneX() - barPressScreenX);
            double dy = Math.abs(e.getSceneY() - barPressScreenY);
            if (dx < 5 && dy < 5) e.consume();
        });
    }

    private static String format(long n) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
        sym.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###");
        df.setDecimalFormatSymbols(sym);
        return df.format(n);
    }

    // Tiny hamburger button that opens the ContextMenu at its right edge
    private StackPane buildHamburgerButton() {
        // three lines
        Region l1 = new Region(), l2 = new Region(), l3 = new Region();
        for (Region r : new Region[]{ l1, l2, l3 }) {
            r.setPrefSize(MENU_ICON - 6, 2);
            r.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            r.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            r.setStyle("-fx-background-color:#3a4147; -fx-background-radius:1;");
        }
        VBox lines = new VBox(4, l1, l2, l3);
        lines.setAlignment(Pos.CENTER);

        // background tile
        Region bg = new Region();
        bg.setPrefSize(MENU_ICON, MENU_ICON);
        bg.setMinSize(MENU_ICON, MENU_ICON);
        bg.setMaxSize(MENU_ICON, MENU_ICON);
        bg.setStyle("""
        -fx-background-color:#bad1e8;
        -fx-background-radius:2;
        -fx-border-color:#3a4147;
        -fx-border-width:1;
        -fx-border-radius:2;
        -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);
    """);

        StackPane button = new StackPane(bg, lines);
        button.setPrefSize(MENU_ICON, MENU_ICON);
        button.setMinSize(MENU_ICON, MENU_ICON);
        button.setMaxSize(MENU_ICON, MENU_ICON);
        button.setPickOnBounds(true);

        // simple hover
        button.setOnMouseEntered(e -> bg.setStyle("""
        -fx-background-color:#cce1ed;
        -fx-background-radius:2;
        -fx-border-color:#3a4147;
        -fx-border-width:1;
        -fx-border-radius:2;
        -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);
    """));
        button.setOnMouseExited(e -> bg.setStyle("""
        -fx-background-color:#bad1e8;
        -fx-background-radius:2;
        -fx-border-color:#3a4147;
        -fx-border-width:1;
        -fx-border-radius:2;
        -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);
    """));

        // open context menu on click (menu is assigned later in ctor; that's fine)
        button.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> e.consume());
        button.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            e.consume();
            if (menu == null) return;
            var bounds = button.localToScreen(button.getBoundsInLocal());
            if (menu.isShowing()) menu.hide();
            menu.show(button, bounds.getMaxX(), bounds.getMinY());
        });

        return button;
    }


}
