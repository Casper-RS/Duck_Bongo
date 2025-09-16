// dev/casperrs/duckbongo/app/DuckOverlay.java
package dev.casperrs.duckbongo.app;
import dev.casperrs.duckbongo.core.PointsManager;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javafx.util.Duration;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;

public class DuckOverlay {
    private static final int DUCK_WIDTH = 140;
    private static final int BAR_WIDTH  = 100;
    private static final int BAR_HEIGHT = 22;
    private static final int OVERLAP    = 25;
    private static final int MENU_ICON  = 22;
    private final ImageView breadIcon;

    // drag state
    // add near your other fields
    private double barPressScreenX, barPressScreenY;
    private double dragOffsetX, dragOffsetY;
    private double pressScreenX, pressScreenY;
    private boolean didDrag;

    private final ContextMenu menu;
    private final Stage stage;
    private final ImageView duck;
    private final StackPane counterBar;  // background + label (+ bubble)
    private final Label counterText;
    private final PointsManager points;


    private String skin = "/assets/skins/duck_default.png";

    public DuckOverlay(Stage stage, PointsManager points) {
        this.stage = stage;
        this.points = points;

        // Duck image
        URL url = Objects.requireNonNull(
                DuckOverlay.class.getResource(skin),
                "Missing resource: " + skin);
        Image img = new Image(url.toExternalForm(), DUCK_WIDTH, 0, true, true);
        this.duck = new ImageView(img);

        // Counter bar
        counterText = new Label(format(points.get()));
        counterText.setStyle("-fx-text-fill:#1f2428;-fx-font-size:12px;-fx-font-weight:bold;");
        this.menu = buildContextMenu();

        // Bread icon
        Image bread = new Image(getClass().getResourceAsStream("/assets/Bread.png"), 32, 32, true, true);
        this.breadIcon = new ImageView(bread);
        breadIcon.setVisible(false); // start hidden

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

        StackPane hamburgerButton = buildHamburgerButton();

        counterBar = new StackPane(bg, counterText);
        StackPane.setAlignment(counterText, Pos.CENTER);
        counterBar.setPadding(new Insets(0));
        counterBar.setPickOnBounds(true);

        // Layout: horizontal row with counter and menu button
        HBox barRow = new HBox(6, counterBar, hamburgerButton);
        barRow.setAlignment(Pos.CENTER);
        barRow.setTranslateY(-OVERLAP);
        duck.setTranslateY(OVERLAP / 3.0);

        // Duck + Bread side by side (bread on left)
        HBox duckWithBread = new HBox(5, breadIcon, duck);
        duckWithBread.setAlignment(Pos.CENTER);

        // Column layout: duck+bread on top, barRow below
        VBox column = new VBox(0, duckWithBread, barRow);
        column.setAlignment(Pos.TOP_LEFT);
        column.setPadding(new Insets(2, 4, 4, 4));

        Group root = new Group(column);

        // Scene
        int sceneW = Math.max(DUCK_WIDTH, BAR_WIDTH + 5 + MENU_ICON) + 20;
        int sceneH = (int) (img.getHeight() + BAR_HEIGHT + 20);
        Scene scene = new Scene(root, sceneW, sceneH);
        scene.setFill(null);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.getIcons().add(new Image("/assets/program_icon.png"));
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);

        // dragging
        enableWindowDrag(scene);
        enableToggleOnBar(counterBar);

        // position
        stage.setX(20);
        stage.setY(Screen.getPrimary().getVisualBounds().getMaxY() - sceneH - 18);

        // bread spawner (every 10 minutes → for testing set to 5s)
        Timeline breadSpawner = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> spawnBread())
        );
        breadSpawner.setCycleCount(Animation.INDEFINITE);
        breadSpawner.play();
    }

    private void spawnBread() {
        breadIcon.setVisible(true);

        // optional: animate it appearing
        FadeTransition fade = new FadeTransition(Duration.seconds(1), breadIcon);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

    }


    public void show() {
        stage.show();
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

    // --- Dragging & click handling ---
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
            // require a bigger movement before we start dragging the window
            if (dx > 8 || dy > 8) {
                stage.setX(e.getScreenX() - dragOffsetX);
                stage.setY(e.getScreenY() - dragOffsetY);
                didDrag = true;
            }
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e ->
                scene.setCursor(javafx.scene.Cursor.DEFAULT)
        );
    }

    // Click bar to toggle live timer (ignore if it was a drag)
    private void enableToggleOnBar(javafx.scene.Node bar) {
        bar.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            barPressScreenX = e.getScreenX();
            barPressScreenY = e.getScreenY();
        });

        bar.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            double dx = Math.abs(e.getScreenX() - barPressScreenX);
            double dy = Math.abs(e.getScreenY() - barPressScreenY);
            // treat as a click only if the bar press & release stayed within ~5px
            if (dx < 5 && dy < 5) {
                e.consume();
            }
        });

}
    // formatting: thin-space grouping (e.g., 1 000 000)
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
        // Drie horizontale lijntjes
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

        // Achtergrond net als counterBar
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

        // StackPane met background + icon
        StackPane button = new StackPane(bg, lines);
        button.setPrefSize(MENU_ICON, MENU_ICON);
        button.setMinSize(MENU_ICON, MENU_ICON);
        button.setMaxSize(MENU_ICON, MENU_ICON);
        button.setPickOnBounds(true);

        // Hover feedback (lichtgrijze overlay)
        button.setOnMouseEntered(e ->
                bg.setStyle("""
                -fx-background-color:#cce1ed;
                -fx-background-radius:2;
                -fx-border-color:#3a4147;
                -fx-border-width:1;
                -fx-border-radius:2;
                -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);
            """)
        );
        button.setOnMouseExited(e ->
                bg.setStyle("""
                -fx-background-color:#bad1e8;
                -fx-background-radius:2;
                -fx-border-color:#9ea1a3;
                -fx-border-width:1;
                -fx-border-radius:2;
                -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);
            """)
        );

        // ContextMenu openen
        button.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            e.consume(); // Voorkomt dat de bar-click dit als klik ziet
        });
        button.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            e.consume();
            var screenBounds = button.localToScreen(button.getBoundsInLocal());
            if (menu.isShowing()) {
                menu.hide();
            }
            menu.show(button, screenBounds.getMaxX(), screenBounds.getMinY());
        });

        return button;
    }

    private void imageSwitcher() {
        URL url = Objects.requireNonNull(
                DuckOverlay.class.getResource(skin),
                "Missing resource: " + skin);
        Image img = new Image(url.toExternalForm(), DUCK_WIDTH, 0, true, true);
        this.duck.setImage(img);
    }

    private void skinSubMenuItems(Menu skinSubMenu) {
        List<String> skins = foreachFileList();
        for (String s : skins) {
            String skinName = s.replace("duck_", "").replace(".png", "").replace("_", " ");
            MenuItem mi = new MenuItem(skinName);

            Image preview = new Image(
                    getClass().getResourceAsStream("/assets/skins/" + s), 32, 32, true, true);

            ImageView icon = new ImageView(preview);
            mi.setGraphic(icon);

            mi.setOnAction(e -> {
                this.skin = "/assets/skins/" + s;
                imageSwitcher();
            });
            skinSubMenu.getItems().add(mi);
        }
    }

    private List <String> foreachFileList() {
        Path folder = Paths.get("src/main/resources/assets/skins");   // your folder path
        List<String> fileNames = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    fileNames.add(path.getFileName().toString());
                }
            }
        } catch (IOException IOE) {
            IOE.printStackTrace();
            return null;
        }
        return fileNames;
    }

    /** ContextMenu met wat handige acties. Pas aan naar wens. */
    private ContextMenu buildContextMenu() {
        MenuItem copyCount = new MenuItem("Copy count");
        copyCount.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(Long.toString(points.get()));
            Clipboard.getSystemClipboard().setContent(content);
        });

        MenuItem toggleTop = new MenuItem("Toggle always-on-top");
        toggleTop.setOnAction(e -> stage.setAlwaysOnTop(!stage.isAlwaysOnTop()));

        MenuItem snapBR = new MenuItem("Snap bottom-right");
        snapBR.setOnAction(e -> snapBottomRight());

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> stage.close());

        Menu skinSubMenu = new Menu("Skins");
        skinSubMenuItems(skinSubMenu);

        ContextMenu cm = new ContextMenu(
                skinSubMenu,
                copyCount,
                toggleTop,
                snapBR,
                exit
        );
        return cm;
    }
}
