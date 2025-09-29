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
    // Keep handle to toggles to reflect programmatic changes
    private CheckMenuItem toggleNamesItem;
    private CheckMenuItem toggleMovementItem;
    // Counter label reference for updates
    private Label counterLabel;

    // Local + remote ducks
    private DuckView localDuck;
    private final Map<Integer, DuckView> otherDucks = new HashMap<>();
    private volatile int myId = -1;
    private boolean initialized = false;

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
    // Track which remote duck we're currently dragging locally
    private volatile int draggingRemoteId = -1;
    // UI preferences
    private boolean showNames = true;
    private volatile boolean movementSyncEnabled = true;

    public DuckOverlay(Stage stage, PointsManager points) {
        this.stage = stage;
        this.points = points;
        
        // Initialize the local duck
        this.localDuck = new DuckView(DUCK_WIDTH, true);
        
        // Compose initial local duck image
        refreshLocalImage();
        
        // Mark as initialized
        this.initialized = true;

        // Counter bar
        Label counterText = new Label(format(points.get()));
        counterText.setStyle("-fx-text-fill:#1f2428;-fx-font-size:12px;-fx-font-weight:bold;");
        this.counterLabel = counterText;
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

        column = new VBox(-32, localStack, barRow);
        column.setAlignment(Pos.TOP_LEFT);
        column.setPadding(new Insets(2, 4, 4, 4));

        othersLayer = new Group();
        Group root = new Group(othersLayer, column);

        // Make the scene span the full screen so ducks can move anywhere
        var vb = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, vb.getWidth(), vb.getHeight());
        scene.setFill(null);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);
        stage.setX(vb.getMinX());
        stage.setY(vb.getMinY());

        enableWindowDrag(scene);
        enableToggleOnBar(counterBar);

        // Initial placement of local duck near bottom-left; users can drag anywhere afterwards
        stage.setX(vb.getMinX());
        stage.setY(vb.getMinY());

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
            if (events != null) {
                float fx = (float) column.getTranslateX();
                float fy = (float) column.getTranslateY();
                events.onPositionChanged(fx, fy);
                events.onPositionSettled(fx, fy);
            }
            isDragging = false;
        });

        // Counter punch animation trigger (simple: expose a method you can call externally)
        this.menu = buildContextMenu(counterText);
    }

    // ========== Public API ==========
    public void setEvents(DuckEvents events) { this.events = events; }
    public void setMyId(int id) {
        this.myId = id;
        // If we rendered our own duck as a "remote" before myId was known, remove it now.
        DuckView ghost = otherDucks.remove(id);
        if (ghost != null && othersLayer != null) {
            othersLayer.getChildren().remove(ghost.node());
        }
    }
    public int getMyId() { return this.myId; }
    public boolean isMovementSyncEnabled() { return movementSyncEnabled; }
    public void setMovementSyncEnabled(boolean enabled) {
        this.movementSyncEnabled = enabled;
        if (toggleMovementItem != null) toggleMovementItem.setSelected(enabled);
    }

    public float getDuckX() { return (float) column.getTranslateX(); }
    public float getDuckY() { return (float) column.getTranslateY(); }
    public String getDuckSkin() { return duckSkin; }
    public String getWaterSkin() { return waterSkin; }
    public double getSceneWidth() {
        Scene sc = stage.getScene();
        return (sc != null) ? sc.getWidth() : Screen.getPrimary().getVisualBounds().getWidth();
    }
    public double getSceneHeight() {
        Scene sc = stage.getScene();
        return (sc != null) ? sc.getHeight() : Screen.getPrimary().getVisualBounds().getHeight();
    }

    public void setShowNames(boolean show) {
        this.showNames = show;
        // Apply to all existing remote ducks
        for (DuckView dv : otherDucks.values()) {
            dv.setNameVisible(showNames);
        }
        if (toggleNamesItem != null) toggleNamesItem.setSelected(showNames);
    }
    public boolean isShowNames() { return showNames; }

    // Set local duck position from code (e.g., sync to server spawn) without causing network spam
    public void setLocalPosition(float x, float y, boolean notify) {
        Platform.runLater(() -> {
            column.setTranslateX(x);
            column.setTranslateY(y);
            if (notify && events != null) events.onPositionChanged(x, y);
        });
    }

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

    // Cache for loaded images to avoid reloading the same images multiple times
    private final Map<String, Image> imageCache = new HashMap<>();
    
    /**
     * Updates the world state with new duck positions and states.
     * Optimized to reduce memory usage and improve performance.
     */
    public void updateWorld(Map<Integer, DuckState> world) {
        if (world == null || !initialized) {
            return;
        }
        
        Platform.runLater(() -> {
            try {
                // Extra safety: if a ghost of *me* exists, remove it now
                DuckView ghost = otherDucks.remove(myId);
                if (ghost != null) {
                    cleanupDuckView(ghost);
                }

                // Process updates for existing ducks and add new ones
                for (Map.Entry<Integer, DuckState> entry : world.entrySet()) {
                    int id = entry.getKey();
                    if (id == myId) continue; // Skip self
                    
                    DuckState state = entry.getValue();
                    DuckView duck = otherDucks.get(id);
                    
                    if (duck == null) {
                        // Create new duck view only when needed
                        createNewDuckView(id, state);
                    } else {
                        // Update existing duck
                        updateExistingDuckView(duck, state, id);
                    }
                }

                // Remove ducks that are no longer in the world
                removeStaleDucks(world);
                
                // Suggest garbage collection if we've removed a lot of ducks
                if (otherDucks.size() < world.size() / 2) {
                    System.gc();
                }
            } catch (Exception e) {
                System.err.println("Error in updateWorld: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Creates a new duck view and sets up its event handlers
     */
    private void createNewDuckView(int id, DuckState state) {
        DuckView dv = new DuckView(DUCK_WIDTH, false);
        try {
            dv.node().setMouseTransparent(false);
            
            // Load and cache images
            String duckPath = ResourceUtils.normDuck(state.skin);
            String waterPath = ResourceUtils.normWater(state.water);
            
            Image duckImg = loadAndCacheImage(duckPath);
            Image waterImg = loadAndCacheImage(waterPath);
            
            // Set up the duck view
            dv.rememberPaths(state.skin, state.water);
            dv.setSkins(duckImg, waterImg);
            dv.setName(state.username);
            dv.setNameVisible(showNames);
            
            // Add to our tracking
            otherDucks.put(id, dv);
            if (othersLayer != null) {
                othersLayer.getChildren().add(dv.node());
            }
            
            // Set initial position
            if (movementSyncEnabled && id != draggingRemoteId) {
                dv.setTranslate(state.x, state.y);
            }
            
            // Set up drag handlers
            setupDuckDragHandlers(dv, id);
            
            if (column != null) {
                column.toFront();
            }
        } catch (Exception e) {
            System.err.println("Failed to create duck view: " + e.getMessage());
            cleanupDuckView(dv);
        }
    }
    
    /**
     * Updates an existing duck view with new state
     */
    private void updateExistingDuckView(DuckView duck, DuckState state, int id) {
        try {
            // Update skins if changed
            if (!Objects.equals(duck.duckPath(), state.skin) || 
                !Objects.equals(duck.waterPath(), state.water)) {
                
                String duckPath = ResourceUtils.normDuck(state.skin);
                String waterPath = ResourceUtils.normWater(state.water);
                
                Image duckImg = loadAndCacheImage(duckPath);
                Image waterImg = loadAndCacheImage(waterPath);
                
                duck.rememberPaths(state.skin, state.water);
                duck.setSkins(duckImg, waterImg);
            }
            
            // Update name and visibility
            duck.setName(state.username);
            duck.setNameVisible(showNames);
            
            // Update position if we're not currently dragging this duck
            if (movementSyncEnabled && id != draggingRemoteId) {
                duck.setTranslate(state.x, state.y);
            }
        } catch (Exception e) {
            System.err.println("Failed to update duck view: " + e.getMessage());
        }
    }
    
    /**
     * Removes ducks that are no longer in the world
     */
    private void removeStaleDucks(Map<Integer, DuckState> world) {
        Iterator<Map.Entry<Integer, DuckView>> it = otherDucks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, DuckView> entry = it.next();
            if (!world.containsKey(entry.getKey())) {
                // Remove from the scene and clean up
                if (othersLayer != null) {
                    othersLayer.getChildren().remove(entry.getValue().node());
                }
                cleanupDuckView(entry.getValue());
                it.remove();
            }
        }
    }
    
    /**
     * Loads an image and caches it for future use
     */
    private Image loadAndCacheImage(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        // Check cache first
        Image cached = imageCache.get(path);
        if (cached != null) {
            return cached;
        }
        
        // Load and cache the image
        Image img = loadFlexible(getClass(), path);
        if (img != null) {
            // Limit cache size to prevent memory issues
            if (imageCache.size() > 50) {
                // Remove the first entry (oldest) if cache is too large
                Iterator<String> it = imageCache.keySet().iterator();
                if (it.hasNext()) {
                    imageCache.remove(it.next());
                }
            }
            imageCache.put(path, img);
        }
        return img;
    }
    
    /**
     * Sets up drag handlers for a duck view
     */
    private void setupDuckDragHandlers(DuckView duck, int id) {
        final double[] press = new double[2];
        final double[] start = new double[2];
        
        duck.node().addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            draggingRemoteId = id;
            press[0] = e.getSceneX();
            press[1] = e.getSceneY();
            start[0] = duck.node().getTranslateX();
            start[1] = duck.node().getTranslateY();
            e.consume();
        });
        
        duck.node().addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double dx = e.getSceneX() - press[0];
            double dy = e.getSceneY() - press[1];
            float nx = (float)(start[0] + dx);
            float ny = (float)(start[1] + dy);
            duck.setTranslate(nx, ny);
            if (events != null) {
                events.onOtherMoved(id, nx, ny);
            }
            e.consume();
        });
        
        duck.node().addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            float nx = (float) duck.node().getTranslateX();
            float ny = (float) duck.node().getTranslateY();
            if (events != null) {
                events.onOtherMoved(id, nx, ny);
                events.onOtherSettled(id, nx, ny);
            }
            draggingRemoteId = -1;
            e.consume();
        });
    }
    
    /**
     * Cleans up resources used by a duck view
     */
    private void cleanupDuckView(DuckView duck) {
        if (duck != null) {
            try {
                duck.close();
            } catch (Exception e) {
                System.err.println("Error cleaning up duck view: " + e.getMessage());
            }
        }
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
        addOne.setOnAction(e -> { points.add(1); updateCounter(points.get()); punch(); });

        MenuItem toggleTop = new MenuItem("Toggle always-on-top");
        toggleTop.setOnAction(e -> stage.setAlwaysOnTop(!stage.isAlwaysOnTop()));

        toggleNamesItem = new CheckMenuItem("Show usernames");
        toggleNamesItem.setSelected(showNames);
        toggleNamesItem.setOnAction(e -> {
            boolean sel = toggleNamesItem.isSelected();
            setShowNames(sel);
            if (events != null) events.onShowNamesChanged(sel);
        });

        toggleMovementItem = new CheckMenuItem("Sync movement with server");
        toggleMovementItem.setSelected(movementSyncEnabled);
        toggleMovementItem.setOnAction(e -> {
            boolean sel = toggleMovementItem.isSelected();
            setMovementSyncEnabled(sel);
            if (events != null) events.onMovementSyncChanged(sel);
        });

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

        return new ContextMenu(setIp, skinPopup, addOne, copyCount, toggleTop, toggleNamesItem, toggleMovementItem, exit);
    }

    private void enableWindowDrag(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            // Only allow window dragging when the press is on the empty scene/root, not on duck nodes
            if (e.getTarget() != scene.getRoot()) return;
            pressScreenX = e.getScreenX(); pressScreenY = e.getScreenY();
            dragOffsetX  = pressScreenX - stage.getX(); dragOffsetY = pressScreenY - stage.getY();
            didDrag = false;
        });
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            // Only move window when dragging the empty scene/root
            if (e.getTarget() != scene.getRoot()) return;
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

    // Public method to update the counter label safely on the FX thread
    public void updateCounter(long value) {
        Platform.runLater(() -> {
            if (counterLabel != null) counterLabel.setText(format(value));
        });
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
