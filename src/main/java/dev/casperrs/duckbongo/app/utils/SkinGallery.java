package dev.casperrs.duckbongo.app.utils;

import dev.casperrs.duckbongo.app.skins.DuckSkin;
import dev.casperrs.duckbongo.app.skins.SkinSelectionListener;
import dev.casperrs.duckbongo.app.skins.WaterSkin;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public final class SkinGallery {

    private static final String CONTAINER_STYLE = """
            -fx-background-color: linear-gradient(to bottom right, #b3ecff, #80dfff, #4dd2ff);
            -fx-background-insets: 0, 1;
            -fx-background-radius: 20;
            -fx-border-color: #ffeb3b;
            -fx-border-radius: 20;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0.3, 0, 3);
            -fx-padding: 15;
            """;

    private static final String HEADER_STYLE = """
            -fx-font-size:16px;
            -fx-font-weight:bold;
            -fx-padding:8 0 4 0;
            -fx-background-color:#eeeeee;
            -fx-border-color:#cccccc;
            -fx-border-width:0 0 1 0;
            """;

    public static void show(Stage owner, double x, double y, SkinSelectionListener listener) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        Text title = new Text("Choose your duck skin");
        title.setStyle("-fx-font-size:16px;-fx-font-weight:bold;");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setStyle(CONTAINER_STYLE);
        content.getChildren().addAll(
                title,
                header("Ducks"),
                duckTile(popup, listener),
                header("Waters"),
                waterTile(popup, listener)
        );

        popup.getContent().setAll(content);
        popup.show(owner, x, y);
    }

    private static Label header(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_LEFT);
        label.setStyle(HEADER_STYLE);
        return label;
    }

    private static TilePane duckTile(Popup popup, SkinSelectionListener listener) {
        TilePane tile = newTile();
        for (DuckSkin skin : DuckSkin.values()) {
            tile.getChildren().add(cell(skin.resourcePath(), skin.displayName(), () -> {
                listener.onDuckPicked(skin.resourcePath());
                popup.hide();
            }));
        }
        return tile;
    }

    private static TilePane waterTile(Popup popup, SkinSelectionListener listener) {
        TilePane tile = newTile();
        for (WaterSkin skin : WaterSkin.values()) {
            tile.getChildren().add(cell(skin.resourcePath(), skin.displayName(), () -> {
                listener.onWaterPicked(skin.resourcePath());
                popup.hide();
            }));
        }
        return tile;
    }

    private static TilePane newTile() {
        TilePane tile = new TilePane();
        tile.setPrefColumns(5);
        tile.setHgap(10);
        tile.setVgap(10);
        tile.setPadding(new Insets(0, 0, 10, 0));
        return tile;
    }

    private static StackPane cell(String resourcePath, String name, Runnable onClick) {
        URL url = Objects.requireNonNull(
                SkinGallery.class.getResource(resourcePath),
                "Missing resource: " + resourcePath
        );
        Image img = new Image(url.toExternalForm(), 64, 64, true, true);
        ImageView iv = new ImageView(img);

        Label label = new Label(name);
        label.setAlignment(Pos.CENTER);

        VBox vbox = new VBox(iv, label);
        vbox.setAlignment(Pos.CENTER);

        StackPane cell = new StackPane(vbox);
        cell.setPadding(new Insets(4));
        cell.setStyle("-fx-background-color: transparent;");
        cell.setOnMouseEntered(e ->
                cell.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 6;"));
        cell.setOnMouseExited(e ->
                cell.setStyle("-fx-background-color: transparent;"));
        cell.setOnMouseClicked(e -> onClick.run());
        return cell;
    }

    private SkinGallery() {}
}
