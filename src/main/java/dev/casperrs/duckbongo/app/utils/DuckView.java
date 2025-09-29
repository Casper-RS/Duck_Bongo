package dev.casperrs.duckbongo.app.utils;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A view component that displays a duck with optional name label.
 * Implements AutoCloseable to ensure proper resource cleanup.
 */
public final class DuckView implements AutoCloseable {
    private final ImageView image;
    private final Label nameLabel;
    private final StackPane root;
    private String duckPath, waterPath;
    private boolean disposed = false;

    public DuckView(double fitWidth, boolean mouseInteractive) {
        this.image = new ImageView();
        this.nameLabel = new Label("");
        
        // Configure image view
        image.setPreserveRatio(true);
        image.setFitWidth(fitWidth);
        image.setMouseTransparent(!mouseInteractive);
        image.setCache(true);  // Enable image caching
        image.setSmooth(true); // Better quality when scaling
        
        // Configure name label
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
        nameLabel.setCache(true); // Cache the label for better performance

        // Create layout
        VBox box = new VBox(-40, nameLabel, image);
        box.setAlignment(Pos.CENTER);
        box.setCache(true); // Cache the VBox for better performance

        this.root = new StackPane(box);
        this.root.setPickOnBounds(false);
        this.root.setMouseTransparent(!mouseInteractive);
        this.root.setCache(true); // Cache the root for better performance
    }

    public Node node() { 
        checkDisposed();
        return root; 
    }

    public void setTranslate(float x, float y) { 
        checkDisposed();
        root.setTranslateX(x); 
        root.setTranslateY(y); 
    }

    public void setSkins(Image duck, Image water) { 
        checkDisposed();
        Image composed = SkinComposer.compose(duck, water);
        // Set image with null check to prevent memory leaks
        if (composed != null) {
            image.setImage(composed);
        }
    }

    public void rememberPaths(String duckPath, String waterPath) { 
        checkDisposed();
        this.duckPath = duckPath; 
        this.waterPath = waterPath; 
    }
    
    public String duckPath()  { 
        checkDisposed();
        return duckPath;  
    }
    
    public String waterPath() { 
        checkDisposed();
        return waterPath; 
    }

    public void setName(String name) {
        checkDisposed();
        if (name == null || name.isBlank()) {
            nameLabel.setText("");
            nameLabel.setVisible(false);
        } else {
            nameLabel.setText(name);
            nameLabel.setVisible(true);
        }
    }

    public void setNameVisible(boolean visible) {
        checkDisposed();
        // Keep the text but toggle visibility
        nameLabel.setVisible(visible && !nameLabel.getText().isBlank());
    }
    
    /**
     * Checks if this view has been disposed.
     * @return true if disposed, false otherwise
     */
    public boolean isDisposed() {
        return disposed;
    }
    
    /**
     * Throws an IllegalStateException if this view has been disposed.
     */
    private void checkDisposed() {
        if (disposed) {
            throw new IllegalStateException("DuckView has been disposed");
        }
    }
    
    /**
     * Releases resources used by this view.
     */
    @Override
    public void close() {
        if (!disposed) {
            // Clear image to free up memory
            image.setImage(null);
            
            // Clear text and graphic
            nameLabel.setText("");
            nameLabel.setGraphic(null);
            
            // Clear references
            duckPath = null;
            waterPath = null;
            
            disposed = true;
        }
    }
}
