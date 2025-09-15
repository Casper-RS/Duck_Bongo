// dev/casperrs/duckbongo/app/DuckOverlay.java
package dev.casperrs.duckbongo.app;
import dev.casperrs.duckbongo.core.PointsManager;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;
import java.util.function.LongSupplier;

public class DuckOverlay {
    private static final int DUCK_WIDTH = 120;
    private static final int BAR_WIDTH  = 100;
    private static final int BAR_HEIGHT = 28;
    private static final int OVERLAP    = 23;

    // drag state
    // add near your other fields
    private double barPressScreenX, barPressScreenY;
    private double dragOffsetX, dragOffsetY;
    private double pressScreenX, pressScreenY;
    private boolean didDrag;

    private final Stage stage;
    private final ImageView duck;
    private final StackPane counterBar;  // background + label (+ bubble)
    private final Label counterText;
    private final PointsManager points;
    private LongSupplier remainingSeconds; // provided by MainApp

    public DuckOverlay(Stage stage, PointsManager points) {
        this.stage = stage;
        this.points = points;

        URL url = Objects.requireNonNull(
                DuckOverlay.class.getResource("/assets/duck_idle.png"),
                "Missing resource: /assets/duck_idle.png");
        Image img = new Image(url.toExternalForm(), DUCK_WIDTH, 0, true, true);
        this.duck = new ImageView(img);

        // Counter bar
        counterText = new Label(format(points.get()));
        counterText.setStyle("-fx-text-fill:#1f2428;-fx-font-size:12px;-fx-font-weight:bold;");

        Region bg = new Region();
        bg.setPrefSize(BAR_WIDTH, BAR_HEIGHT);
        bg.setMinSize(BAR_WIDTH, BAR_HEIGHT);
        bg.setMaxSize(BAR_WIDTH, BAR_HEIGHT);
        bg.setStyle("""
          -fx-background-color:#dfe6ed;
          -fx-background-radius:8;
          -fx-border-color:#3a4147;
          -fx-border-width:2;
          -fx-border-radius:8;
          -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);
        """);

        counterBar = new StackPane(bg, counterText);
        counterBar.setPadding(new Insets(0));
        counterBar.setPickOnBounds(true);
        counterBar.setTranslateY(-OVERLAP);
        duck.setTranslateY(OVERLAP / 3.0);

        // Layout
        VBox column = new VBox(0, duck, counterBar);
        column.setAlignment(Pos.CENTER);
        column.setPadding(new Insets(4, 8, 8, 8));
        Group root = new Group(column);

        int sceneW = Math.max(DUCK_WIDTH, BAR_WIDTH) + 20;
        int sceneH = (int) (img.getHeight() + BAR_HEIGHT + 28);
        Scene scene = new Scene(root, sceneW, sceneH);
        scene.setFill(null);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);

        // dragging
        enableWindowDrag(scene);
        enableToggleOnBar(counterBar);

        // position
        stage.setX(20);
        stage.setY(Screen.getPrimary().getVisualBounds().getMaxY() - sceneH - 18);
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

    public void refresh() { counterText.setText(format(points.get())); }

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
        sym.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("#,###");
        df.setDecimalFormatSymbols(sym);
        return df.format(n);
    }
}
