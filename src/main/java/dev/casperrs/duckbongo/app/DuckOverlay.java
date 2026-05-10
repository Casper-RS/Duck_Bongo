package dev.casperrs.duckbongo.app;

import dev.casperrs.duckbongo.app.controller.WindowDragController;
import dev.casperrs.duckbongo.app.skins.SkinSelectionListener;
import dev.casperrs.duckbongo.app.skins.SkinSet;
import dev.casperrs.duckbongo.app.ui.CounterBar;
import dev.casperrs.duckbongo.app.ui.DuckContextMenu;
import dev.casperrs.duckbongo.app.ui.HamburgerButton;
import dev.casperrs.duckbongo.core.PointsManager;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.util.Objects;

public class DuckOverlay {
    private static final int DUCK_WIDTH = 140;
    private static final int BAR_WIDTH  = 100;
    private static final int BAR_HEIGHT = 22;
    private static final int OVERLAP    = 25;
    private static final int MENU_ICON  = 22;

    private final Stage stage;
    private final PointsManager points;
    private final ImageView duck = new ImageView();
    private final ImageView breadIcon;
    private final CounterBar counterBar;

    private SkinSet skins = SkinSet.DEFAULT;

    public DuckOverlay(Stage stage, PointsManager points) {
        this.stage = stage;
        this.points = points;

        renderSkin();

        this.counterBar = new CounterBar(BAR_WIDTH, BAR_HEIGHT, points.get());

        Image bread = new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/assets/Bread.png"),
                        "Missing resource: /assets/Bread.png"),
                32, 32, true, true);
        this.breadIcon = new ImageView(bread);
        breadIcon.setVisible(false);

        SkinSelectionListener skinListener = new SkinSelectionListener() {
            @Override public void onDuckPicked(String duckPath) {
                skins = skins.withDuck(duckPath);
                renderSkin();
            }
            @Override public void onWaterPicked(String waterPath) {
                skins = skins.withWater(waterPath);
                renderSkin();
            }
        };

        HamburgerButton hamburger = new HamburgerButton(MENU_ICON,
                () -> DuckContextMenu.build(stage, counterBar, skinListener));

        HBox barRow = new HBox(6, counterBar.node(), hamburger.node());
        barRow.setAlignment(Pos.CENTER);
        barRow.setTranslateY(-OVERLAP);
        duck.setTranslateY(OVERLAP / 3.0);

        Image composite = duck.getImage();
        double displayedHeight = DUCK_WIDTH * composite.getHeight() / composite.getWidth();

        StackPane duckWithBread = new StackPane(duck, breadIcon);
        duckWithBread.setMinSize(DUCK_WIDTH, displayedHeight);
        duckWithBread.setPrefSize(DUCK_WIDTH, displayedHeight);
        duckWithBread.setMaxSize(DUCK_WIDTH, displayedHeight);
        duckWithBread.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(breadIcon, Pos.CENTER_LEFT);
        breadIcon.setTranslateX(6);
        breadIcon.setTranslateY(-4);
        breadIcon.setPickOnBounds(false);

        VBox column = new VBox(0, duckWithBread, barRow);
        column.setAlignment(Pos.TOP_LEFT);
        column.setPadding(new Insets(2, 4, 4, 4));

        Group root = new Group(column);

        int sceneW = Math.max(DUCK_WIDTH, BAR_WIDTH + 5 + MENU_ICON) + 20;
        int sceneH = (int) (displayedHeight + BAR_HEIGHT + 20);
        Scene scene = new Scene(root, sceneW, sceneH);
        scene.setFill(null);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.getIcons().add(new Image("/assets/program_icon.png"));
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);

        WindowDragController.install(stage, scene);

        stage.setX(20);
        stage.setY(Screen.getPrimary().getVisualBounds().getMaxY() - sceneH - 18);

        Timeline breadSpawner = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> spawnBread()));
        breadSpawner.setCycleCount(Animation.INDEFINITE);
        breadSpawner.play();
    }

    public void show() { stage.show(); }

    /** Punch animation + update counter text */
    public void punch() {
        ScaleTransition st = new ScaleTransition(Duration.millis(80), duck);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(0.92);  st.setToY(0.92);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
        counterBar.setValue(points.get());
    }

    private void spawnBread() {
        breadIcon.setVisible(true);
        FadeTransition fade = new FadeTransition(Duration.seconds(1), breadIcon);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void renderSkin() {
        URL duckURL  = Objects.requireNonNull(getClass().getResource(skins.duckPath()),
                "Missing duck resource: " + skins.duckPath());
        URL waterURL = Objects.requireNonNull(getClass().getResource(skins.waterPath()),
                "Missing water resource: " + skins.waterPath());
        Image duckImg  = new Image(duckURL.toExternalForm(),  0, 0, true, true, false);
        Image waterImg = new Image(waterURL.toExternalForm(), 0, 0, true, true, false);

        double width  = Math.max(duckImg.getWidth(),  waterImg.getWidth());
        double height = Math.max(duckImg.getHeight(), waterImg.getHeight());

        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.drawImage(waterImg, 0, 0);
        gc.drawImage(duckImg,  0, 0);

        WritableImage combined = new WritableImage((int) width, (int) height);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        canvas.snapshot(params, combined);

        duck.setImage(combined);
        duck.setFitWidth(DUCK_WIDTH);
        duck.setPreserveRatio(true);
    }
}
