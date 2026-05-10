package dev.casperrs.duckbongo.app.ui;

import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Supplier;

public final class HamburgerButton {

    private static final String STYLE_NORMAL = base("#bad1e8");
    private static final String STYLE_HOVER  = base("#cce1ed");

    private static String base(String color) {
        return """
                -fx-background-color:%s;
                -fx-background-radius:2;
                -fx-border-color:#3a4147;
                -fx-border-width:1;
                -fx-border-radius:2;
                -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0.2, 0, 1);
                """.formatted(color);
    }

    private final StackPane node;

    public HamburgerButton(int size, Supplier<ContextMenu> menuSupplier) {
        Region l1 = line(size), l2 = line(size), l3 = line(size);
        VBox lines = new VBox(4, l1, l2, l3);
        lines.setAlignment(Pos.CENTER);

        Region bg = new Region();
        fix(bg, size, size);
        bg.setStyle(STYLE_NORMAL);

        this.node = new StackPane(bg, lines);
        fix(node, size, size);
        node.setPickOnBounds(true);

        node.setOnMouseEntered(e -> bg.setStyle(STYLE_HOVER));
        node.setOnMouseExited (e -> bg.setStyle(STYLE_NORMAL));

        node.addEventHandler(MouseEvent.MOUSE_PRESSED, MouseEvent::consume);
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            e.consume();
            ContextMenu menu = menuSupplier.get();
            if (menu == null) return;
            var bounds = node.localToScreen(node.getBoundsInLocal());
            if (menu.isShowing()) menu.hide();
            menu.show(node, bounds.getMaxX(), bounds.getMinY());
        });
    }

    public StackPane node() { return node; }

    private static Region line(int size) {
        Region r = new Region();
        fix(r, size - 6, 2);
        r.setStyle("-fx-background-color:#3a4147; -fx-background-radius:1;");
        return r;
    }

    private static void fix(Region r, int w, int h) {
        r.setPrefSize(w, h);
        r.setMinSize(w, h);
        r.setMaxSize(w, h);
    }
}
