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
import javafx.stage.Popup;
import javafx.stage.Window;

public final class SkinGallery {

    private static final int THUMB_PX = 64;

    public static void show(Window owner, double x, double y, SkinSelectionListener cb) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-padding: 8;");

        TilePane ducks = grid();
        for (DuckSkin s : DuckSkin.values()) {
            ducks.getChildren().add(cell(s.resourcePath(), s.displayName(), () -> {
                cb.onDuckPicked(s.resourcePath());
                popup.hide();
            }));
        }

        TilePane waters = grid();
        for (WaterSkin s : WaterSkin.values()) {
            waters.getChildren().add(cell(s.resourcePath(), s.displayName(), () -> {
                cb.onWaterPicked(s.resourcePath());
                popup.hide();
            }));
        }

        content.getChildren().addAll(section("Ducks", ducks), section("Waters", waters));
        popup.getContent().setAll(content);
        popup.show(owner, x, y);
    }

    private static VBox section(String title, TilePane grid) {
        Label header = new Label(title);
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 4 0 6 0;");
        return new VBox(header, grid);
    }

    private static TilePane grid() {
        TilePane grid = new TilePane(10, 10);
        grid.setPrefColumns(5);
        return grid;
    }

    private static StackPane cell(String resourcePath, String label, Runnable onPick) {
        Image img = ResourceUtils.loadFlexible(SkinGallery.class, resourcePath);
        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);
        iv.setFitWidth(THUMB_PX);
        iv.setFitHeight(THUMB_PX);

        VBox box = new VBox(iv, new Label(label));
        box.setAlignment(Pos.CENTER);

        StackPane cell = new StackPane(box);
        cell.setPadding(new Insets(4));
        cell.setOnMouseClicked(e -> onPick.run());
        return cell;
    }

    private SkinGallery() {}
}
