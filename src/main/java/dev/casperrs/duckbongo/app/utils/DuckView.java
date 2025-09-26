package dev.casperrs.duckbongo.app.utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public final class DuckView {
    private final ImageView view = new ImageView();
    private String duckPath, waterPath;

    public DuckView(double fitWidth, boolean mouseInteractive) {
        view.setPreserveRatio(true);
        view.setFitWidth(fitWidth);
        view.setMouseTransparent(!mouseInteractive);
    }

    public ImageView node() { return view; }

    public void setTranslate(float x, float y) { view.setTranslateX(x); view.setTranslateY(y); }

    public void setSkins(Image duck, Image water) { view.setImage(SkinComposer.compose(duck, water)); }

    public void rememberPaths(String duckPath, String waterPath) { this.duckPath = duckPath; this.waterPath = waterPath; }
    public String duckPath()  { return duckPath;  }
    public String waterPath() { return waterPath; }
}
