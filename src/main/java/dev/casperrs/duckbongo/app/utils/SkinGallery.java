package dev.casperrs.duckbongo.app.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Popup;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public final class SkinGallery {
    public interface OnPick { void duck(String duckPath); void water(String waterPath); }

    public static void show(javafx.stage.Window owner, double x, double y, OnPick cb) {
        Popup popup = new Popup(); popup.setAutoHide(true);
        VBox content = new VBox(10); content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-padding: 8;");

        content.getChildren().addAll(
                section("Ducks", "src/main/resources/assets/skin_parts/ducks", true, cb, popup),
                section("Waters", "src/main/resources/assets/skin_parts/waters", false, cb, popup)
        );

        popup.getContent().setAll(content);
        popup.show(owner, x, y);
    }

    private static VBox section(String title, String folder, boolean duck, OnPick cb, Popup popup) {
        Label header = new Label(title);
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 4 0 6 0;");
        TilePane grid = new TilePane(10, 10); grid.setPrefColumns(5);

        for (String s : files(folder)) {
            Path file = Paths.get(folder, s);
            ImageView iv = new ImageView(new Image(file.toUri().toString(), 64, 64, true, true));
            VBox box = new VBox(iv, new Label(s.replaceFirst("^(duck_|water_)", "").replace(".png","")));
            box.setAlignment(Pos.CENTER);
            StackPane cell = new StackPane(box);
            cell.setPadding(new Insets(4));
            cell.setOnMouseClicked(e -> {
                if (duck) cb.duck("/assets/skin_parts/ducks/" + s);
                else      cb.water("/assets/skin_parts/waters/" + s);
                popup.hide();
            });
            grid.getChildren().add(cell);
        }
        return new VBox(header, grid);
    }

    private static List<String> files(String folder) {
        List<String> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(folder))) {
            for (Path p : ds) if (Files.isRegularFile(p)) out.add(p.getFileName().toString());
        } catch (Exception ignored) {}
        return out;
    }

    private SkinGallery() {}
}
