package dev.casperrs.duckbongo.app;

import dev.casperrs.duckbongo.core.PointsManager;
import dev.casperrs.duckbongo.dataHandler.DataHandler;
import dev.casperrs.duckbongo.network.DuckState;
import dev.casperrs.duckbongo.network.WorldState;
import dev.casperrs.duckbongo.input.InputHook;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;
import java.util.HashMap;

public class MainApp extends Application {

    private DuckOverlay overlay;
    private long lastPointsSeen = 0;
    private final PointsManager points = new PointsManager();
    private final DataHandler dataHandler = new DataHandler(points);
    private InputHook inputHook;
    private Client client;
    private boolean connected = false;
    private volatile String serverIP = "13.62.96.190";

    @Override
    public void start(Stage stage) {
        // Setup overlay
        overlay = new DuckOverlay(stage, points);

        // Load persisted points and user data
        dataHandler.initAndLoad();

        // Start global input hook so clicks/keys add points
        try {
            inputHook = new InputHook(points, null); // no-op activity updater
            inputHook.start();
        } catch (Exception e) {
            System.out.println("⚠️ Failed to start input hook: " + e.getMessage());
        }

        // Send immediate updates while dragging and when skin changes
        overlay.setOnPositionChanged((x, y) -> {
            if (connected && client != null) {
                DuckState me = new DuckState();
                me.x = x;
                me.y = y;
                me.skin = overlay.getDuckSkin(); // current local skin
                me.water = overlay.getWaterSkin(); // current local water
                client.sendTCP(me);
            }
        });
        overlay.setOnSkinChanged(skin -> {
            if (connected && client != null) {
                System.out.println("[Client] onSkinChanged -> " + skin);
                DuckState me = new DuckState();
                me.x = overlay.getDuckX();
                me.y = overlay.getDuckY();
                me.skin = skin; // new skin picked
                me.water = overlay.getWaterSkin();
                client.sendTCP(me);
            }
        });
        overlay.setOnWaterChanged(water -> {
            if (connected && client != null) {
                System.out.println("[Client] onWaterChanged -> " + water);
                DuckState me = new DuckState();
                me.x = overlay.getDuckX();
                me.y = overlay.getDuckY();
                me.skin = overlay.getDuckSkin();
                me.water = water; // new water picked
                client.sendTCP(me);
            }
        });

        // Allow changing server IP from overlay menu
        overlay.setOnServerIpSubmit(ip -> {
            System.out.println("🔁 Reconnecting to server at " + ip + "...");
            connectToServer(ip);
        });

        // Start initial connection
        connectToServer(serverIP);

        // Main update loop
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Send duck state to server if connected (periodic heartbeat)
                if (connected && client != null) {
                    DuckState me = new DuckState();
                    me.x = overlay.getDuckX();
                    me.y = overlay.getDuckY();
                    me.skin = overlay.getDuckSkin();
                    me.water = overlay.getWaterSkin();
                    client.sendTCP(me);
                }

                // Update points animation if changed
                long currentPoints = points.get();
                if (currentPoints != lastPointsSeen) {
                    overlay.punch();
                    lastPointsSeen = currentPoints;
                }
            }
        }.start();
    }

    private synchronized void connectToServer(String ip) {
        // Update target IP
        this.serverIP = ip;

        // Stop previous client if any
        if (client != null) {
            try {
                client.stop();
            } catch (Exception ignored) {}
            connected = false;
        }

        new Thread(() -> {
            try {
                client = new Client();
                client.start();

                // Configure Kryo serialization
                Kryo kryo = client.getKryo();
                kryo.register(DuckState.class);
                kryo.register(WorldState.class);
                kryo.register(HashMap.class);

                // Handle server messages
                client.addListener(new Listener() {
                    @Override
                    public void received(Connection c, Object obj) {
                        if (obj instanceof WorldState worldState) {
                            // Update overlay with world state on JavaFX thread
                            Platform.runLater(() -> overlay.updateWorld(worldState.ducks));
                        }
                    }

                    @Override
                    public void connected(Connection c) {
                        connected = true;
                        // Inform overlay of our assigned connection ID
                        Platform.runLater(() -> overlay.setMyId(c.getID()));
                        System.out.println("✅ Connected to DuckBongo server!");

                        // Bootstrap: send current state once on connect so others see us immediately
                        DuckState me = new DuckState();
                        me.x = overlay.getDuckX();
                        me.y = overlay.getDuckY();
                        me.skin = overlay.getDuckSkin();
                        me.water = overlay.getWaterSkin();
                        client.sendTCP(me);
                    }

                    @Override
                    public void disconnected(Connection c) {
                        connected = false;
                        System.out.println("❌ Disconnected from server");
                    }
                });

                // Try to connect to server
                System.out.println("🔄 Connecting to server at " + this.serverIP + ":54555...");
                client.connect(5000, this.serverIP, 54555, 54777);



            } catch (IOException e) {
                System.err.println("❌ Failed to connect to server: " + e.getMessage());
                System.out.println("💡 Make sure the DuckBongo server is running!");
                connected = false;
            }
        }, "NetworkingThread").start();
    }

    @Override
    public void stop() {
        if (dataHandler != null) {
            dataHandler.save();
        }

        if (inputHook != null) {
            try { inputHook.stop(); } catch (Exception ignored) {}
        }

        if (client != null) {
            client.stop();
        }

        System.out.println("👋 DuckBongo client shutting down...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}