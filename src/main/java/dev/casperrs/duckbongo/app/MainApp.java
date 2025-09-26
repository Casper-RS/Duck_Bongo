package dev.casperrs.duckbongo.app;

import dev.casperrs.duckbongo.app.utils.DuckEvents;
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
    private volatile boolean connected = false;
    private volatile String serverIP = "13.62.96.190";

    // Optional: tiny throttle if you want to send heartbeat occasionally (ms)
    private static final long HEARTBEAT_MS = 0; // set to 0 to disable heartbeat
    private long lastHeartbeat = 0;

    @Override
    public void start(Stage stage) {
        // === UI / Overlay ===
        overlay = new DuckOverlay(stage, points);

        // === Persistence ===
        dataHandler.initAndLoad();

        // === Input Hook (adds points) ===
        try {
            inputHook = new InputHook(points, null); // no rich presence updater
            inputHook.start();
        } catch (Exception e) {
            System.out.println("⚠️ Failed to start input hook: " + e.getMessage());
        }

        // === Wire overlay → network (local authority) ===
        overlay.setEvents(new DuckEvents() {
            @Override
            public void onPositionChanged(float x, float y) {
                if (!connected || client == null) return;
                DuckState me = new DuckState();
                me.x = x;
                me.y = y;
                me.skin = overlay.getDuckSkin();
                me.water = overlay.getWaterSkin();
                client.sendTCP(me);
            }

            @Override
            public void onDuckSkinChanged(String duckPath) {
                if (!connected || client == null) return;
                System.out.println("[Client] onSkinChanged -> " + duckPath);
                DuckState me = new DuckState();
                me.x = overlay.getDuckX();
                me.y = overlay.getDuckY();
                me.skin = duckPath;
                me.water = overlay.getWaterSkin();
                client.sendTCP(me);
            }

            @Override
            public void onWaterSkinChanged(String waterPath) {
                if (!connected || client == null) return;
                System.out.println("[Client] onWaterChanged -> " + waterPath);
                DuckState me = new DuckState();
                me.x = overlay.getDuckX();
                me.y = overlay.getDuckY();
                me.skin = overlay.getDuckSkin();
                me.water = waterPath;
                client.sendTCP(me);
            }

            @Override
            public void onServerIpSubmit(String ip) {
                System.out.println("🔁 Reconnecting to server at " + ip + "...");
                connectToServer(ip);
            }
        });


        // === Connect ===
        connectToServer(serverIP);

        // === Lightweight loop (no per-frame network spam) ===
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Optional heartbeat (disabled by default)
                if (HEARTBEAT_MS > 0 && connected && client != null) {
                    long ms = System.currentTimeMillis();
                    if (ms - lastHeartbeat >= HEARTBEAT_MS) {
                        lastHeartbeat = ms;
                        DuckState me = new DuckState();
                        me.x = overlay.getDuckX();
                        me.y = overlay.getDuckY();
                        me.skin = overlay.getDuckSkin();
                        me.water = overlay.getWaterSkin();
                        client.sendTCP(me);
                    }
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
        this.serverIP = ip;

        // Stop old client if any
        if (client != null) {
            try { client.stop(); } catch (Exception ignored) {}
            connected = false;
        }

        new Thread(() -> {
            try {
                client = new Client(16384, 4096);
                client.start();

                // IMPORTANT: registration order must match the server
                Kryo kryo = client.getKryo();
                kryo.register(HashMap.class);
                kryo.register(DuckState.class);
                kryo.register(WorldState.class);

                client.addListener(new Listener() {
                    @Override
                    public void received(Connection connection, Object object) {
                        if (object instanceof WorldState worldState && worldState.ducks != null) {
                            // overlay.updateWorld(...) already does Platform.runLater internally,
                            // but calling it directly is also fine if you prefer to keep UI-only there.
                            overlay.updateWorld(worldState.ducks);
                        }
                    }

                    @Override
                    public void connected(Connection c) {
                        connected = true;
                        Platform.runLater(() -> overlay.setMyId(c.getID()));
                        System.out.println("✅ Connected to DuckBongo server!");

                        // Bootstrap: send my current state so others see me immediately
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
            try { client.stop(); } catch (Exception ignored) {}
        }
        System.out.println("👋 DuckBongo client shutting down...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
