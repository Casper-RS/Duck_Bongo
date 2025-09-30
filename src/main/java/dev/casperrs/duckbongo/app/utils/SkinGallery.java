package dev.casperrs.duckbongo.app.utils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Cursor;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public final class SkinGallery {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();
    private static List<String> cachedDuckSkins = null;
    private static List<String> cachedWaterSkins = null;
    
    public interface OnPick { void duck(String duckPath); void water(String waterPath); }

    // Calculate the number of columns based on available width
    private static final int COLUMN_WIDTH = 80; // Width of each skin column (including gap)
    private static final int MIN_COLUMNS = 4;   // Minimum number of columns to show
    
    public static void show(javafx.stage.Window owner, double x, double y, OnPick cb) {
        show(owner, x, y, cb, Collections.emptySet(), Collections.emptySet());
    }

    public static void show(javafx.stage.Window owner, double x, double y, OnPick cb,
                            Collection<String> unlockedPaths, Collection<String> lockedPaths) {
        Popup popup = new Popup(); 
        popup.setAutoHide(true);
        
        // Main content container with scroll
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(520);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: transparent;");
        
        // Content container
        VBox content = new VBox(20);
        content.setPadding(new Insets(15));
        content.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 5;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);"
        );
        
        // Add loading indicator
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        content.getChildren().add(loadingIndicator);
        
        // Set up the scroll pane content
        scrollPane.setContent(content);
        
        // Show popup immediately with loading state
        popup.getContent().add(scrollPane);
        
        // Calculate available width for columns
        double popupWidth = 450; // Default width
        int numColumns = Math.max(MIN_COLUMNS, (int)((popupWidth - 40) / COLUMN_WIDTH)); // Account for padding and scrollbar
        
        // Normalize locked/unlocked collections
        Set<String> unlockedSet = unlockedPaths == null ? Collections.emptySet() : new HashSet<>(unlockedPaths);
        Set<String> lockedSet = lockedPaths == null ? Collections.emptySet() : new HashSet<>(lockedPaths);

        // Load skins in background
        executor.submit(() -> {
            try {
                // Load file lists in parallel
                List<String> ducks = cachedDuckSkins != null ? cachedDuckSkins : 
                    files("/assets/skin_parts/ducks");
                List<String> waters = cachedWaterSkins != null ? cachedWaterSkins : 
                    files("/assets/skin_parts/waters");
                
                // Cache the file lists
                cachedDuckSkins = ducks;
                cachedWaterSkins = waters;
                
                // Create UI on JavaFX thread
                Platform.runLater(() -> {
                    content.getChildren().clear();
                    
                    // Add sections with consistent styling
                    VBox ducksSection = createSection("Ducks", "/assets/skin_parts/ducks", true,
                            cb, popup, ducks, numColumns, unlockedSet, lockedSet);
                    VBox watersSection = createSection("Waters", "/assets/skin_parts/waters", false,
                            cb, popup, waters, numColumns, unlockedSet, lockedSet);
                    
                    // Add some spacing between sections
                    ducksSection.setPadding(new Insets(0, 0, 10, 0));
                    
                    content.getChildren().addAll(ducksSection, watersSection);
                    
                    // Set a reasonable size for the popup
                    double contentHeight = content.prefHeight(-1);
                    if (Double.isNaN(contentHeight) || contentHeight <= 0) {
                        contentHeight = 480; // Reasonable fallback height
                    }
                    double popupHeight = Math.min(650, Math.max(420, contentHeight + 40));
                    scrollPane.setPrefSize(popupWidth, popupHeight);
                    scrollPane.setMinHeight(popupHeight);
                    scrollPane.setMaxHeight(650);
                    
                    // Position the popup relative to mouse cursor
                    double screenWidth = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
                    double screenHeight = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
                    
                    double popupX = x;
                    double popupY = y;
                    
                    // Adjust position if popup would go off-screen
                    if (popupX + popupWidth > screenWidth) {
                        popupX = screenWidth - popupWidth - 10; // 10px margin from right
                    }
                    if (popupY + popupHeight > screenHeight) {
                        popupY = screenHeight - popupHeight - 10; // 10px margin from bottom
                    }
                    
                    // Ensure popup is fully on screen
                    popupX = Math.max(10, popupX);
                    popupY = Math.max(10, popupY);
                    
                    // Show the popup at the calculated position
                    popup.show(owner, popupX, popupY);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(popup::hide);
            }
        });
    }
    
    private static VBox createSection(String title, String folder, boolean duck, OnPick cb, 
                                     Popup popup, List<String> skinFiles, int numColumns,
                                     Set<String> unlocked, Set<String> locked) {
        VBox section = section(title, folder, duck, cb, popup, skinFiles, numColumns, unlocked, locked);
        section.setMaxWidth(Double.MAX_VALUE);
        return section;
    }

    private static VBox section(String title, String folder, boolean duck, OnPick cb, Popup popup,
                                List<String> skinFiles, int numColumns,
                                Set<String> unlocked, Set<String> locked) {
        Label header = new Label(title);
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 4 0 6 0;");
        
        // Create the grid with consistent column layout
        TilePane grid = new TilePane(10, 10);
        grid.setPrefColumns(numColumns);
        grid.setVgap(15);
        grid.setHgap(10);
        grid.setPadding(new Insets(5, 0, 5, 0));
        grid.setPrefTileWidth(64);
        grid.setPrefTileHeight(100);
        grid.setStyle("-fx-border-color: transparent;");
        
        // Add all cells first with loading state
        Map<String, Node> cellMap = new LinkedHashMap<>();
        for (String skinFile : skinFiles) {
            // Create cell with loading state
            Label nameLabel = new Label(skinFile.replaceFirst("^(duck_|water_)", "").replace(".png", ""));
            nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

            StackPane imageContainer = new StackPane();
            imageContainer.setMinSize(64, 64);
            imageContainer.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #eee; -fx-border-width: 1;");

            VBox box = new VBox(4, imageContainer, nameLabel);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(5));

            StackPane cell = new StackPane(box);
            cell.setPadding(new Insets(4));
            cell.getStyleClass().add("skin-cell");

            final String cpPath = folder + "/" + skinFile;
            boolean isUnlocked;
            boolean hasUnlocked = unlocked != null && !unlocked.isEmpty();
            boolean hasLocked = locked != null && !locked.isEmpty();

            if (hasUnlocked) {
                isUnlocked = unlocked.contains(cpPath);
            } else if (hasLocked) {
                isUnlocked = !locked.contains(cpPath);
            } else {
                isUnlocked = false; // default to locked until we know otherwise
            }

            cell.setCursor(isUnlocked ? Cursor.HAND : Cursor.DEFAULT);
            cell.setOpacity(isUnlocked ? 1.0 : 0.4);

            cell.setOnMouseEntered(e -> {
                if (isUnlocked) {
                    cell.setStyle("-fx-background-color: #e0e0e0;");
                }
            });
            cell.setOnMouseExited(e -> cell.setStyle(""));

            final String skinFilePath = skinFile; // Create final copy for use in lambda
            cell.setOnMouseClicked(e -> {
                if (!isUnlocked) {
                    e.consume();
                    return;
                }
                if (duck) cb.duck("/assets/skin_parts/ducks/" + skinFilePath);
                else cb.water("/assets/skin_parts/waters/" + skinFilePath);
                popup.hide();
            });

            // Add loading shimmer
            StackPane loadingShimmer = createLoadingPlaceholder();
            cell.getChildren().add(loadingShimmer);

            if (!isUnlocked) {
                Label lockedBadge = new Label("Locked");
                lockedBadge.setStyle("-fx-background-color: rgba(0,0,0,0.65); -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 2 6 2 6; -fx-background-radius: 12;");
                StackPane.setAlignment(lockedBadge, Pos.TOP_RIGHT);
                StackPane.setMargin(lockedBadge, new Insets(4));
                cell.getChildren().add(lockedBadge);
            }

            // Store reference to update later
            cellMap.put(skinFile, cell);
            grid.getChildren().add(cell);
        }
        
        // Load images in background
        for (String skinFile : skinFiles) {
            final String cpPath = folder + "/" + skinFile;
            Node cell = cellMap.get(skinFile);
            if (cell == null) continue;
            
            // Get the image container (first child of the first child)
            StackPane imageContainer = (StackPane) ((VBox) ((StackPane) cell).getChildren().get(0)).getChildren().get(0);
            
            loadImageAsync(cpPath, 64, 64, true, true, image -> {
                if (image != null) {
                    ImageView iv = new ImageView(image);
                    iv.setFitHeight(64);
                    iv.setPreserveRatio(true);
                    
                    Platform.runLater(() -> {
                        // Remove loading shimmer
                        if (cell instanceof Pane) {
                            Pane pane = (Pane) cell;
                            pane.getChildren().removeIf(node -> node.getStyleClass() != null && 
                                node.getStyleClass().contains("loading-shimmer"));
                        }
                        
                        // Add the image
                        imageContainer.getChildren().clear();
                        imageContainer.getChildren().add(iv);
                    });
                }
            });
        }
        
        VBox container = new VBox(8, header, grid);
        container.setFillWidth(true);
        container.setStyle(
            "-fx-background-color: white;" +
            "-fx-padding: 10 15 10 15;" +
            "-fx-background-radius: 5;"
        );
        
        // Add subtle border between sections
        if (duck) {
            container.setStyle(container.getStyle() + 
                "-fx-border-color: #f0f0f0;" +
                "-fx-border-width: 0 0 1 0;"
            );
        }
        
        return container;
    }
    
    private static StackPane createLoadingPlaceholder() {
        StackPane placeholder = new StackPane();
        placeholder.getStyleClass().add("loading-shimmer");
        placeholder.setMinSize(64, 64);
        placeholder.setStyle(
            "-fx-background-color: #f0f0f0; " +
            "-fx-border-color: #e0e0e0; " +
            "-fx-border-width: 1; "
        );
        
        // Add shimmer effect
        Region shimmer = new Region();
        shimmer.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(255,255,255,0) 0%, " +
            "rgba(255,255,255,0.5) 50%, rgba(255,255,255,0) 100%);"
        );
        shimmer.setOpacity(0.5);
        shimmer.setMouseTransparent(true);
        
        placeholder.getChildren().add(shimmer);
        
        // Animate shimmer
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(shimmer.translateXProperty(), -64)),
            new KeyFrame(Duration.seconds(1.5), new KeyValue(shimmer.translateXProperty(), 128))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        
        // Stop animation when placeholder is removed
        placeholder.parentProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                timeline.stop();
            }
        });
        
        return placeholder;
    }

    private static void loadImageAsync(String path, int width, int height, boolean preserveRatio, 
                                     boolean smooth, java.util.function.Consumer<Image> callback) {
        String cacheKey = path + "@" + width + "x" + height;
        
        // Check cache first
        Image cached = imageCache.get(cacheKey);
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        
        // Load in background
        executor.submit(() -> {
            try {
                URL res = SkinGallery.class.getResource(path);
                if (res != null) {
                    Image img = new Image(res.toExternalForm(), width, height, preserveRatio, smooth);
                    if (!img.isError()) {
                        imageCache.put(cacheKey, img);
                        Platform.runLater(() -> callback.accept(img));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Returns all skin resources within the given folder, prefixed with the folder path.
     * Example input: "/assets/skin_parts/ducks"
     */
    public static List<String> listSkins(String folder) {
        return files(folder).stream()
            .map(name -> folder + "/" + name)
            .collect(Collectors.toList());
    }

    private static List<String> files(String folder) {
        // folder should be a classpath resource path like "/assets/skin_parts/ducks"
        List<String> out = new ArrayList<>();
        try {
            URL url = SkinGallery.class.getResource(folder);
            if (url == null) return out;
            
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                // Running from IDE or exploded resources - use NIO for better performance
                Path dir = Paths.get(url.toURI());
                if (Files.isDirectory(dir)) {
                    try (var stream = Files.list(dir)) {
                        out = stream
                            .filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().endsWith(".png"))
                            .map(p -> p.getFileName().toString())
                            .sorted()
                            .collect(Collectors.toList());
                    }
                }
            } else if ("jar".equals(protocol)) {
                // Running from packaged JAR; enumerate entries
                // url example: jar:file:/C:/path/app.jar!/assets/skin_parts/ducks
                String path = url.getPath();
                int sep = path.indexOf("!/");
                if (sep > 0) {
                    String jarPath = path.substring(0, sep);
                    if (jarPath.startsWith("file:")) {
                        jarPath = jarPath.substring(5);
                    }
                    jarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8);
                    
                    String prefix = folder.startsWith("/") ? folder.substring(1) : folder;
                    String prefixWithSlash = prefix + "/";
                    
                    try (JarFile jf = new JarFile(new File(jarPath))) {
                        out = jf.versionedStream()
                            .filter(je -> !je.isDirectory())
                            .map(JarEntry::getName)
                            .filter(name -> name.startsWith(prefixWithSlash))
                            .filter(name -> name.endsWith(".png"))
                            .map(name -> name.substring(prefixWithSlash.length()))
                            .filter(name -> !name.contains("/")) // Only immediate children
                            .sorted()
                            .collect(Collectors.toList());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading skins from " + folder + ": " + e.getMessage());
        }
        return out;
    }

    private SkinGallery() {}
}
