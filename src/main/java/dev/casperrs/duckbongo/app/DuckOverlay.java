package dev.casperrs.duckbongo.app;

import dev.casperrs.duckbongo.core.PointsManager;
import dev.casperrs.duckbongo.network.DuckState;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class DuckOverlay {

    private static final int DUCK_WIDTH = 140;
    private static final int BAR_WIDTH  = 100;
    private static final int BAR_HEIGHT = 22;
    private static final int OVERLAP    = 25;
    private static final int MENU_ICON  = 22;

    private final PointsManager points;
    private final Stage stage;

    private final ImageView duck;
    private final ImageView breadIcon;
    private final StackPane counterBar;  // background + label
    private final Label counterText;
    private final ContextMenu menu;

    // Networking helpers
    private volatile int myId = -1; // server-assigned connection id
    private final HashMap<Integer, ImageView> otherDucks = new HashMap<>();
    private java.util.function.Consumer<String> onServerIpSubmit;
    // Notify MainApp when local skin or position changes so it can send to server
    // private java.util.function.Consumer<String> onSkinChanged;
    // private java.util.function.BiConsumer<Float, Float> onPositionChanged;
    // Notify MainApp when local skin/water or position changes so it can send to server
private java.util.function.Consumer<String> onSkinChanged;
private java.util.function.Consumer<String> onWaterChanged;
private java.util.function.BiConsumer<Float, Float> onPositionChanged;

    // Dragging state (window + duck)
    private double barPressScreenX, barPressScreenY;
    private double dragOffsetX, dragOffsetY;
    private double pressScreenX, pressScreenY;
    private boolean didDrag;
    private boolean isDragging;

    // Cooldown to ignore server echo on skin immediately after local change
    private long lastLocalSkinChangeMs = 0L;

    // Skins
    private String waterSkin = "/assets/skin_parts/waters/water_default.png";
    private String duckSkin  = "/assets/skin_parts/ducks/duck_default.png";

    // Root movable container for the overlay window
    private Group movable;
    // Layer that holds only remote ducks
    private Group othersLayer;
    // Local UI (your duck + counter) container
    private VBox column;

    public DuckOverlay(Stage stage, PointsManager points) {
        this.stage = stage;
        this.points = points;

        // Build main duck (initialize first, then set image via imageSwitcher)
        this.duck = new ImageView();
        duck.setFitWidth(DUCK_WIDTH);
        duck.setPreserveRatio(true);
        duck.setPickOnBounds(true);
        imageSwitcher();

        // Bread icon
        Image bread = new Image(Objects.requireNonNull(getClass().getResource("/assets/Bread.png")).toExternalForm(), 32, 32, true, true);
        this.breadIcon = new ImageView(bread);
        breadIcon.setVisible(false); // start hidden
        breadIcon.setTranslateX(6);
        breadIcon.setTranslateY(-4);
        breadIcon.setPickOnBounds(false);

        // Counter bar
        counterText = new Label(format(points.get()));
        counterText.setStyle("-fx-text-fill:#1f2428;-fx-font-size:12px;-fx-font-weight:bold;");
        Region bg = new Region();
        bg.setPrefSize(BAR_WIDTH, BAR_HEIGHT);
        bg.setMinSize(BAR_WIDTH, BAR_HEIGHT);
        bg.setMaxSize(BAR_WIDTH, BAR_HEIGHT);
        bg.setStyle("""
            -fx-background-color:#bad1e8;
            -fx-background-radius:8;
            -fx-border-color:#3a4147;
            -fx-border-width:1;
            -fx-border-radius:9;
            -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);
        """);
        counterBar = new StackPane(bg, counterText);
        StackPane.setAlignment(counterText, Pos.CENTER);
        counterBar.setPadding(new Insets(0));
        counterBar.setPickOnBounds(true);

        // Hamburger button
        StackPane hamburgerButton = buildHamburgerButton();

        // Layout: row with counter + menu
        HBox barRow = new HBox(6, counterBar, hamburgerButton);
        barRow.setAlignment(Pos.CENTER);
        barRow.setTranslateY(0);
        duck.setTranslateY(0);

        // Top stack with duck + bread
        StackPane duckWithBread = new StackPane();
        duckWithBread.getChildren().addAll(duck, breadIcon);
        duckWithBread.setAlignment(Pos.CENTER_LEFT);

        // Build layers: others below, your UI above
        column = new VBox(0, duckWithBread, barRow);
        column.setAlignment(Pos.TOP_LEFT);
        column.setPadding(new Insets(2, 4, 4, 4));
        othersLayer = new Group();
        movable = new Group(othersLayer, column);
        Group root = new Group(movable);

        // Scene
        int NUDGE_MAX = 120; // headroom for remote-duck nudge
        int sceneW = Math.max(DUCK_WIDTH + NUDGE_MAX, BAR_WIDTH + 5 + MENU_ICON) + 20;
        int sceneH = DUCK_WIDTH + BAR_HEIGHT + 60;
        Scene scene = new Scene(root, sceneW, sceneH);
        scene.setFill(null);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.getIcons().add(new Image("/assets/program_icon.png"));
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);

        // Drag window
        enableWindowDrag(scene);
        enableToggleOnBar(counterBar);

        // Place near bottom-left (like classic)
        stage.setX(20);
        stage.setY(Screen.getPrimary().getVisualBounds().getMaxY() - sceneH - 18);

        // Ensure your local UI starts within the window; remote ducks stay independent
        column.setTranslateX(10);
        column.setTranslateY(10);

        // Show
        stage.show();

        // Drag to change position of your local UI only (not other players)
        final double[] press = new double[2];
        final double[] start = new double[2];

        duck.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            press[0] = e.getSceneX();
            press[1] = e.getSceneY();
            start[0] = column.getTranslateX();
            start[1] = column.getTranslateY();
            isDragging = true;
            // Keep local duck and its UI above everything else
            duck.toFront();
            column.toFront();
            e.consume();
        });

        duck.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double dx = e.getSceneX() - press[0];
            double dy = e.getSceneY() - press[1];
            column.setTranslateX(start[0] + dx);
            column.setTranslateY(start[1] + dy);
            if (onPositionChanged != null) {
                onPositionChanged.accept((float) column.getTranslateX(), (float) column.getTranslateY());
            }
            e.consume();
        });

        duck.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (onPositionChanged != null) {
                onPositionChanged.accept((float) column.getTranslateX(), (float) column.getTranslateY());
            }
            isDragging = false;
        });

        // Periodic bread spawn (as before, demo)
        javafx.animation.Timeline breadSpawner = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(5), e -> spawnBread())
        );
        breadSpawner.setCycleCount(javafx.animation.Animation.INDEFINITE);
        breadSpawner.play();

        // Context menu
        this.menu = buildContextMenu();
    }

    // -------- Networking integration --------
    public void setMyId(int id) { this.myId = id; }
    public void setOnServerIpSubmit(java.util.function.Consumer<String> consumer) { this.onServerIpSubmit = consumer; }
    // public void setOnSkinChanged(java.util.function.Consumer<String> consumer) { this.onSkinChanged = consumer; }
    // public void setOnPositionChanged(java.util.function.BiConsumer<Float, Float> consumer) { this.onPositionChanged = consumer; }
    // public float getDuckX() { return (float) column.getTranslateX(); }
    // public float getDuckY() { return (float) column.getTranslateY(); }
    // public String getDuckSkin() { return duckSkin; }

    public void setOnSkinChanged(java.util.function.Consumer<String> consumer) { this.onSkinChanged = consumer; }
public void setOnWaterChanged(java.util.function.Consumer<String> consumer) { this.onWaterChanged = consumer; }
public void setOnPositionChanged(java.util.function.BiConsumer<Float, Float> consumer) { this.onPositionChanged = consumer; }
public float getDuckX() { return (float) column.getTranslateX(); }
public float getDuckY() { return (float) column.getTranslateY(); }
public String getDuckSkin() { return duckSkin; }
public String getWaterSkin() { return waterSkin; }

    public void updateWorld(HashMap<Integer, DuckState> worldDucks) {
        // Update our duck from network state (single source of truth)
        DuckState me = worldDucks.get(myId);
        if (me != null) {
            // Do not override local drag with server echo while dragging
            if (!isDragging) {
                column.setTranslateX(me.x);
                column.setTranslateY(me.y);
            }
            if (me.skin != null) {
                String norm = normalizeDuckPath(me.skin);
                // Ignore server skin echo for a short cooldown after a local change
                long now = System.currentTimeMillis();
                if (now - lastLocalSkinChangeMs >= 300) {
                    if (!Objects.equals(duckSkin, norm)) {
                        duckSkin = norm;
                        imageSwitcher();
                        // IMPORTANT: do not call onSkinChanged here to avoid echo loops
                    }
                }
            }
            if (me.water != null) {
    String wnorm = normalizeWaterPath(me.water);
    if (!Objects.equals(waterSkin, wnorm)) {
        waterSkin = wnorm;
        imageSwitcher();
    }
}
        }

        // Update or create other ducks
        for (var entry : worldDucks.entrySet()) {
            int id = entry.getKey();
            DuckState state = entry.getValue();
            if (id == myId) continue; // skip local, handled above

            ImageView view = otherDucks.get(id);
            if (view == null) {
Image img = composeImage(normalizeDuckPath(state.skin), normalizeWaterPath(state.water));                view = new ImageView(img);
                view.setFitWidth(DUCK_WIDTH);
                view.setPreserveRatio(true);
                // Remote ducks should not intercept mouse so you can drag your own duck
                view.setMouseTransparent(true);
                view.setPickOnBounds(false);
                otherDucks.put(id, view);
                // Add to remote layer behind your UI so your duck/counter stay on top
                othersLayer.getChildren().add(view);
                duck.toFront();
                // Remember last applied skin (raw string from server for change detection)
                view.getProperties().put("skin", state.skin);
                view.getProperties().put("water", state.water);
            }
            // If their skin changed, re-compose their image
            // Object lastSkin = view.getProperties().get("skin");
            // if (lastSkin == null || !lastSkin.equals(state.skin)) {
            //     String norm = normalizeDuckPath(state.skin);
            //     view.setImage(composeImage(norm, waterSkin));
            //     view.getProperties().put("skin", state.skin);
            // }

            Object lastSkin = view.getProperties().get("skin");
Object lastWater = view.getProperties().get("water");
if (lastSkin == null || !lastSkin.equals(state.skin) || lastWater == null || !lastWater.equals(state.water)) {
    String norm = normalizeDuckPath(state.skin);
    String wnorm = normalizeWaterPath(state.water);
    view.setImage(composeImage(norm, wnorm));
    view.getProperties().put("skin", state.skin);
    view.getProperties().put("water", state.water);
}

            // Avoid overlap with local duck: nudge remote duck if it's too close to ours
            float ox = (float) column.getTranslateX();
            float oy = (float) column.getTranslateY();
            float rx = state.x;
            float ry = state.y;

            // Distance from our duck
            float dx = rx - ox;
            float dy = ry - oy;

            // Slightly larger window to avoid overlap after updates
            if (Math.abs(dx) < 100f && Math.abs(dy) < 60f) {
                float base = 100f;                 // chosen base offset
                float spread = (id % 3) * 12f;     // small per-id spread
                float offset = base + spread;

                // If nudging right would push out of scene bounds, nudge left instead
                double maxX = stage.getWidth() - DUCK_WIDTH - 10; // keep a small right margin
                if (rx + offset > maxX) {
                    rx = Math.max(10f, rx - offset);              // nudge left with a left margin
                } else {
                    rx = rx + offset;                             // nudge right
                }
            }

            view.setTranslateX(rx);
            view.setTranslateY(ry);
        }

        // Remove disconnected ducks (two-phase to avoid ConcurrentModificationException)
        java.util.List<Integer> stale = new java.util.ArrayList<>();
        for (Integer id : otherDucks.keySet()) {
            if (!worldDucks.containsKey(id)) {
                stale.add(id);
            }
        }
        for (Integer id : stale) {
            ImageView iv = otherDucks.remove(id);
            if (iv != null) othersLayer.getChildren().remove(iv);
        }
    }

    // -------- Classic behaviors --------
    public void show() { stage.show(); }

    private void spawnBread() {
        breadIcon.setVisible(true);
        FadeTransition fade = new FadeTransition(Duration.seconds(1), breadIcon);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    /** Punch animation + update counter text */
    public void punch() {
        ScaleTransition st = new ScaleTransition(Duration.millis(80), duck);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(0.92);  st.setToY(0.92);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
        counterText.setText(format(points.get()));
    }

    private void enableWindowDrag(Scene scene) {
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            pressScreenX = e.getScreenX();
            pressScreenY = e.getScreenY();
            dragOffsetX  = pressScreenX - stage.getX();
            dragOffsetY  = pressScreenY - stage.getY();
            didDrag = false;
        });
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
            double dx = Math.abs(e.getScreenX() - pressScreenX);
            double dy = Math.abs(e.getScreenY() - pressScreenY);
            if (dx > 8 || dy > 8) {
                stage.setX(e.getScreenX() - dragOffsetX);
                stage.setY(e.getScreenY() - dragOffsetY);
                didDrag = true;
            }
        });
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e -> scene.setCursor(javafx.scene.Cursor.DEFAULT));
    }

    private void enableToggleOnBar(javafx.scene.Node bar) {
        bar.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            barPressScreenX = e.getScreenX();
            barPressScreenY = e.getScreenY();
        });
        bar.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            double dx = Math.abs(e.getSceneX() - barPressScreenX);
            double dy = Math.abs(e.getSceneY() - barPressScreenY);
            if (dx < 5 && dy < 5) {
                e.consume();
            }
        });
    }

    // Formatting: thin-space grouping (e.g., 1 000 000)
    private static String format(long n) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
        sym.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###");
        df.setDecimalFormatSymbols(sym);
        return df.format(n);
    }

    private void snapBottomRight() {
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        stage.setX(vb.getMaxX() - stage.getWidth() - 20);
        stage.setY(vb.getMaxY() - stage.getHeight() - 18);
    }

    private StackPane buildHamburgerButton() {
        Region line1 = new Region();
        Region line2 = new Region();
        Region line3 = new Region();
        for (Region r : new Region[]{line1, line2, line3}) {
            r.setPrefSize(MENU_ICON - 5, 1);
            r.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            r.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            r.setStyle("-fx-background-color:#3a4147; -fx-background-radius:1;");
        }
        VBox lines = new VBox(4, line1, line2, line3);
        lines.setAlignment(Pos.CENTER);

        Region bg = new Region();
        bg.setPrefSize(MENU_ICON, MENU_ICON);
        bg.setMinSize(MENU_ICON, MENU_ICON);
        bg.setMaxSize(MENU_ICON, MENU_ICON);
        bg.setStyle("""
            -fx-background-color:#bad1e8;
            -fx-background-radius:1;
            -fx-border-color:#3a4147;
            -fx-border-width:1;
            -fx-border-radius:1;
            -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);
        """);

        StackPane button = new StackPane(bg, lines);
        button.setPrefSize(MENU_ICON, MENU_ICON);
        button.setMinSize(MENU_ICON, MENU_ICON);
        button.setMaxSize(MENU_ICON, MENU_ICON);
        button.setPickOnBounds(true);

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
            -fx-border-color:#9ea1a3;
            -fx-border-width:1;
            -fx-border-radius:2;
            -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);
        """));

        button.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> e.consume());
        button.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            e.consume();
            var screenBounds = button.localToScreen(button.getBoundsInLocal());
            if (menu.isShowing()) menu.hide();
            menu.show(button, screenBounds.getMaxX(), screenBounds.getMinY());
        });
        return button;
    }

    // Compose and set duck image (water + duck), return composed image
    private Image imageSwitcher() {
        Image duckImg  = loadFlexible(duckSkin);
        Image waterImg = loadFlexible(waterSkin);
        Image composed = compose(duckImg, waterImg);

        duck.setImage(composed);
        duck.setFitWidth(DUCK_WIDTH);
        duck.setPreserveRatio(true);
        // Keep local duck and UI above others after skin swap (but only after column is built)
        duck.toFront();
        if (column != null) {
            column.toFront();
        }
        return composed;
    }

    private Image composeImage(String duckPath, String waterPath) {
        Image duckImg  = loadFlexible(duckPath);
        Image waterImg = loadFlexible(waterPath);
        return compose(duckImg, waterImg);
    }

    private Image compose(Image duckImg, Image waterImg) {
        double width  = Math.max(duckImg.getWidth(), waterImg.getWidth());
        double height = Math.max(duckImg.getHeight(), waterImg.getHeight());
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.drawImage(waterImg, 0, 0);
        gc.drawImage(duckImg, 0, 0);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);
        WritableImage combined = new WritableImage((int) width, (int) height);
        return canvas.snapshot(params, combined);
    }

    private Image loadFlexible(String path) {
        try {
            if (path != null) {
                var res = getClass().getResource(path);
                if (res != null) return new Image(res.toExternalForm(), 0, 0, true, true);
                if (path.startsWith("file:")) return new Image(path, 0, 0, true, true);
                File f = new File(path);
                if (f.exists()) return new Image(f.toURI().toString(), 0, 0, true, true);
            }
        } catch (Exception ignored) { }
        // Fallback to default duck if nothing else worked
        String fallback = "/assets/skin_parts/ducks/duck_default.png";
        var res = Objects.requireNonNull(getClass().getResource(fallback));
        return new Image(res.toExternalForm(), 0, 0, true, true);
    }

    private String normalizeDuckPath(String p) {
        if (p == null || p.isBlank()) return "/assets/skin_parts/ducks/duck_default.png";
        if (p.startsWith("file:")) return p;                 // file URI
        if (p.startsWith("/assets/")) return p;               // classpath asset
        if (!p.contains("/")) return "/assets/skin_parts/ducks/" + p; // bare filename
        return p;
    }

    private String normalizeWaterPath(String p) {
    if (p == null || p.isBlank()) return "/assets/skin_parts/waters/water_default.png";
    if (p.startsWith("file:")) return p;
    if (p.startsWith("/assets/")) return p;
    if (!p.contains("/")) return "/assets/skin_parts/waters/" + p;
    return p;
}

    private List<String> foreachFileList(String folderPath) {
        Path folder = Paths.get(folderPath);
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) fileNames.add(path.getFileName().toString());
            }
        } catch (Exception ignored) { }
        return fileNames;
    }

    /** ContextMenu with handy actions + Skin Gallery popup */
    private ContextMenu buildContextMenu() {
        MenuItem copyCount = new MenuItem("Copy count");
        copyCount.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(Long.toString(points.get()));
            Clipboard.getSystemClipboard().setContent(content);
        });

        MenuItem addOne = new MenuItem("Add 1 bread (test)");
        addOne.setOnAction(e -> { points.add(1); punch(); });

        MenuItem toggleTop = new MenuItem("Toggle always-on-top");
        toggleTop.setOnAction(e -> stage.setAlwaysOnTop(!stage.isAlwaysOnTop()));

        MenuItem snapBR = new MenuItem("Snap bottom-right");
        snapBR.setOnAction(e -> snapBottomRight());

        MenuItem setIp = new MenuItem("Set Server IP...");
        setIp.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Server IP");
            dlg.setHeaderText("Connect to server");
            dlg.setContentText("Enter server IP:");
            dlg.showAndWait().ifPresent(ip -> {
                if (onServerIpSubmit != null && ip != null && !ip.isBlank()) onServerIpSubmit.accept(ip.trim());
            });
        });

        MenuItem skinPopup = new MenuItem("Skin Gallery...");
        skinPopup.setOnAction(e -> showSkinPopup(stage, stage.getX() + 50, stage.getY() + 50));

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> { stage.close(); Platform.exit(); System.exit(0); });

        return new ContextMenu(setIp, skinPopup, addOne, copyCount, toggleTop, snapBR, exit);
    }

    public void showSkinPopup(Stage owner, double x, double y) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        TilePane tile = new TilePane();
        tile.setPrefColumns(5);
        tile.setHgap(10);
        tile.setVgap(10);
        tile.setPadding(new Insets(10));

        String duckyStyle = """
            -fx-background-color: linear-gradient(to bottom right, #b3ecff, #80dfff, #4dd2ff);
            -fx-background-insets: 0, 1;
            -fx-background-radius: 20;
            -fx-border-color: #ffeb3b;
            -fx-border-radius: 20;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0.3, 0, 3);
            -fx-padding: 15;
        """;

        VBox box = new VBox();
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: white; -fx-border-color: black;");
        Text title = new Text("Choose your duck skin");
        title.setStyle("-fx-font-size:16px;-fx-font-weight:bold;");
        box.getChildren().add(title);

        VBox root = new VBox(10, box, tile);
        root.setPadding(new Insets(10));
        root.setStyle(duckyStyle);

        popup.getContent().clear();
        popup.getContent().add(root);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white;");

        // Ducks
        loadSkins("src/main/resources/assets/skin_parts/ducks", popup, content);
        // Waters
        loadSkins("src/main/resources/assets/skin_parts/waters", popup, content);

        content.setStyle(duckyStyle);
        popup.getContent().setAll(content);
        popup.show(owner, x + 200, y - 150);
    }

    private void loadSkins(String folder, Popup popup, VBox parentBox) {
        String section = Path.of(folder).getFileName().toString(); // "ducks" or "waters"
        Label header = new Label(section.substring(0,1).toUpperCase() + section.substring(1));
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("""
            -fx-font-size:16px;
            -fx-font-weight:bold;
            -fx-padding:8 0 4 0;
            -fx-background-color:#eeeeee;
            -fx-border-color:#cccccc;
            -fx-border-width:0 0 1 0;
        """);

        TilePane tile = new TilePane();
        tile.setPrefColumns(5);
        tile.setHgap(10);
        tile.setVgap(10);
        tile.setPadding(new Insets(0,0,10,0));

        List<String> files = foreachFileList(folder);
        if (files != null) {
            for (String s : files) {
                Path file = Paths.get(folder, s);
                Image img = new Image(file.toUri().toString(), 64, 64, true, true);
                ImageView iv = new ImageView(img);
                Label label = new Label(s.replaceFirst("^(duck_|water_)", "").replace(".png", ""));
                label.setAlignment(Pos.CENTER);
                VBox vbox = new VBox(iv, label);
                vbox.setAlignment(Pos.CENTER);
                StackPane cell = new StackPane(vbox);
                cell.setPadding(new Insets(4));
                cell.setStyle("-fx-background-color: transparent;");
                cell.setOnMouseEntered(e -> cell.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 6;"));
                cell.setOnMouseExited(e -> cell.setStyle("-fx-background-color: transparent;"));
                cell.setOnMouseClicked(e -> {
                    if (section.equals("ducks")) {
                        duckSkin = "/assets/skin_parts/ducks/" + s;
                        imageSwitcher();
                        lastLocalSkinChangeMs = System.currentTimeMillis(); // cooldown start
                        if (onSkinChanged != null) onSkinChanged.accept(duckSkin);
                    } else if (section.equals("waters")) {
    waterSkin = "/assets/skin_parts/waters/" + s;
    imageSwitcher();
    if (onWaterChanged != null) onWaterChanged.accept(waterSkin);
}
                    popup.hide();
                });
                tile.getChildren().add(cell);
            }
        }
        parentBox.getChildren().addAll(header, tile);
    }
}