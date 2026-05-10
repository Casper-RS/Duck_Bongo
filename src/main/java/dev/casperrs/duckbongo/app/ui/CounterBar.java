package dev.casperrs.duckbongo.app.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class CounterBar {

    private static final String STYLE = """
            -fx-background-color:#bad1e8;
            -fx-background-radius:8;
            -fx-border-color:#3a4147;
            -fx-border-width:1;
            -fx-border-radius:9;
            -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);
            """;

    private final StackPane node;
    private final Label label;

    public CounterBar(int width, int height, long initialValue) {
        this.label = new Label(Numbers.grouped(initialValue));
        label.setStyle("-fx-text-fill:#1f2428;-fx-font-size:12px;-fx-font-weight:bold;");

        Region bg = new Region();
        bg.setPrefSize(width, height);
        bg.setStyle(STYLE);

        this.node = new StackPane(bg, label);
        node.setPadding(Insets.EMPTY);
    }

    public StackPane node()   { return node; }
    public Label label()      { return label; }

    public void setValue(long n) { label.setText(Numbers.grouped(n)); }
}
