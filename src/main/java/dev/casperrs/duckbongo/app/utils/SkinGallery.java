package dev.casperrs.duckbongo.app.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Popup;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class SkinGallery {
    public interface OnPick { void duck(String duckPath); void water(String waterPath); }

    public static void show(javafx.stage.Window owner, double x, double y, OnPick cb) {
        Popup popup = new Popup(); popup.setAutoHide(true);
        VBox content = new VBox(10); content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-padding: 8;");

        content.getChildren().addAll(
                section("Ducks", "/assets/skin_parts/ducks", true, cb, popup),
                section("Waters", "/assets/skin_parts/waters", false, cb, popup)
        );

        popup.getContent().setAll(content);
        popup.show(owner, x, y);
    }

    private static VBox section(String title, String folder, boolean duck, OnPick cb, Popup popup) {
        Label header = new Label(title);
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 4 0 6 0;");
        TilePane grid = new TilePane(10, 10); grid.setPrefColumns(5);

        for (String s : files(folder)) {
            String cpPath = folder + "/" + s;
            URL res = SkinGallery.class.getResource(cpPath);
            if (res == null) continue;
            ImageView iv = new ImageView(new Image(res.toExternalForm(), 64, 64, true, true));
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
        // folder should be a classpath resource path like "/assets/skin_parts/ducks"
        List<String> out = new ArrayList<>();
        try {
            URL url = SkinGallery.class.getResource(folder);
            if (url == null) return out;
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                // Running from IDE or exploded resources
                Path dir = Paths.get(url.toURI());
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.png")) {
                    for (Path p : ds) if (Files.isRegularFile(p)) out.add(p.getFileName().toString());
                }
            } else if ("jar".equals(protocol)) {
                // Running from packaged JAR; enumerate entries
                // url example: jar:file:/C:/path/app.jar!/assets/skin_parts/ducks
                String path = url.getPath();
                int sep = path.indexOf("!/");
                String jarPath = path.substring(0, sep);
                if (jarPath.startsWith("file:")) jarPath = jarPath.substring(5);
                jarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8);
                String prefix = folder.startsWith("/") ? folder.substring(1) : folder; // assets/skin_parts/ducks
                try (JarFile jf = new JarFile(new File(jarPath))) {
                    Enumeration<JarEntry> en = jf.entries();
                    while (en.hasMoreElements()) {
                        JarEntry je = en.nextElement();
                        String name = je.getName();
                        if (je.isDirectory()) continue;
                        if (!name.startsWith(prefix + "/")) continue;
                        if (!name.endsWith(".png")) continue;
                        // Only immediate children (no nested dirs)
                        String rest = name.substring((prefix + "/").length());
                        if (rest.contains("/")) continue;
                        out.add(rest);
                    }
                }
            }
            // sort for stable order
            Collections.sort(out);
        } catch (Exception ignored) {}
        return out;
    }

    private SkinGallery() {}
}
