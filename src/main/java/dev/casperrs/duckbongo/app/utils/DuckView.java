package dev.casperrs.duckbongo.app.utils;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class DuckView {
    private final ImageView image = new ImageView();
    private final Label nameLabel = new Label("");
    private final StackPane root;
    private String duckPath, waterPath;

    public DuckView(double fitWidth, boolean mouseInteractive) {
        image.setPreserveRatio(true);
        image.setFitWidth(fitWidth);
        image.setMouseTransparent(!mouseInteractive);

        nameLabel.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-color: rgba(0,0,0,0.65);" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 2 8 2 8;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);"
        );
        nameLabel.setVisible(false);
        nameLabel.setMouseTransparent(true);

        VBox box = new VBox(-40, nameLabel, image);
        box.setAlignment(Pos.CENTER);

        root = new StackPane(box);
        root.setPickOnBounds(false);
        root.setMouseTransparent(!mouseInteractive);
    }

    public Node node() { return root; }

    public void setTranslate(float x, float y) { root.setTranslateX(x); root.setTranslateY(y); }

    public void setSkins(Image duck, Image water) { image.setImage(SkinComposer.compose(duck, water)); }

    public void rememberPaths(String duckPath, String waterPath) { this.duckPath = duckPath; this.waterPath = waterPath; }
    public String duckPath()  { return duckPath;  }
    public String waterPath() { return waterPath; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            nameLabel.setText("");
            nameLabel.setVisible(false);
        } else {
            nameLabel.setText(name);
            nameLabel.setVisible(true);
        }
    }

    public void setNameVisible(boolean visible) {
        // Keep the text but toggle visibility
        nameLabel.setVisible(visible && !nameLabel.getText().isBlank());
    }
}
