package dev.casperrs.duckbongo.app.utils;

import dev.casperrs.duckbongo.app.skins.SkinSet;
import javafx.scene.image.ImageView;

public final class DuckView {

    private final ImageView view = new ImageView();
    private SkinSet skins = SkinSet.DEFAULT;

    public DuckView(double fitWidth, boolean mouseInteractive) {
        view.setPreserveRatio(true);
        view.setFitWidth(fitWidth);
        view.setMouseTransparent(!mouseInteractive);
    }

    public ImageView node() { return view; }
    public SkinSet skins()  { return skins; }

    public double translateX() { return view.getTranslateX(); }
    public double translateY() { return view.getTranslateY(); }

    public void setTranslate(float x, float y) {
        view.setTranslateX(x);
        view.setTranslateY(y);
    }

    public void setSkins(SkinSet next) {
        this.skins = next;
        view.setImage(SkinComposer.compose(getClass(), next));
    }
}
