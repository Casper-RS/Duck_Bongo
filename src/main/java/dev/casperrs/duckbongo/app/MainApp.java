package dev.casperrs.duckbongo.app;

import dev.casperrs.duckbongo.app.utils.DuckEvents;
import dev.casperrs.duckbongo.core.PointsManager;
import dev.casperrs.duckbongo.dataHandler.DataHandler;
import dev.casperrs.duckbongo.network.DuckState;
import dev.casperrs.duckbongo.network.WorldState;
import dev.casperrs.duckbongo.network.MoveOther;
import dev.casperrs.duckbongo.input.InputHook;
import dev.casperrs.duckbongo.network.AssignId;

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
import java.util.Map;

public class MainApp extends Application {

    private DuckOverlay overlay;
    private long lastPointsSeen = 0;
    private final PointsManager points = new PointsManager();
    private final DataHandler dataHandler = new DataHandler(points);
    private InputHook inputHook;
    private Client client;
    private volatile boolean connected = false;
    private volatile String serverIP = "localhost"; //13.62.96.190

    // Optional: tiny throttle if you want to send heartbeat occasionally (ms)
    private static final long HEARTBEAT_MS = 0; // set to 0 to disable heartbeat
    private long lastHeartbeat = 0;

    // Throttle movement packets so other clients see smoother updates (about 20 Hz)
    private static final long MOVE_SEND_INTERVAL_MS = 50;
    private long lastMoveSentMs = 0;
    // Per-target throttle for moving other users' ducks
    private final Map<Integer, Long> lastOtherMoveSentMs = new HashMap<>();

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
                long now = System.currentTimeMillis();
                if (now - lastMoveSentMs < MOVE_SEND_INTERVAL_MS) return;
                lastMoveSentMs = now;
                DuckState me = new DuckState();
                me.x = x;
                me.y = y;
                me.skin = overlay.getDuckSkin();
                me.water = overlay.getWaterSkin();
                client.sendUDP(me); // send movement over UDP
            }

            @Override
            public void onPositionSettled(float x, float y) {
                if (!connected || client == null) return;
                // Send an immediate final pose to ensure others have the exact resting position
                DuckState me = new DuckState();
                me.x = x;
                me.y = y;
                me.skin = overlay.getDuckSkin();
                me.water = overlay.getWaterSkin();
                me.settled = true;
                client.sendUDP(me);
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
            @Override
            public void onOtherMoved(int targetId, float x, float y) {
                if (!connected || client == null) return;
                long now = System.currentTimeMillis();
                long last = lastOtherMoveSentMs.getOrDefault(targetId, 0L);
                if (now - last < MOVE_SEND_INTERVAL_MS) return;
                lastOtherMoveSentMs.put(targetId, now);
                MoveOther msg = new MoveOther();
                msg.targetId = targetId;
                msg.x = x;
                msg.y = y;
                msg.settled = false;
                client.sendUDP(msg);
            }

            @Override
            public void onOtherSettled(int targetId, float x, float y) {
                if (!connected || client == null) return;
                MoveOther msg = new MoveOther();
                msg.targetId = targetId;
                msg.x = x;
                msg.y = y;
                msg.settled = true;
                client.sendUDP(msg);
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

                Kryo kryo = client.getKryo();
                // MUST match server order:
                kryo.register(HashMap.class);
                kryo.register(DuckState.class);
                kryo.register(WorldState.class);
                kryo.register(AssignId.class);
                kryo.register(MoveOther.class);

                client.addListener(new Listener() {
                    @Override
                    public void received(Connection connection, Object object) {
                        if (object instanceof AssignId a) {
                            // Use server-assigned id
                            Platform.runLater(() -> overlay.setMyId(a.id));
                            return;
                        }
                        if (object instanceof WorldState ws && ws.ducks != null) {
                            // Sync my own local duck to the server-assigned position once myId is known
                            int myId = overlay.getMyId();
                            if (myId >= 0) {
                                DuckState mine = ws.ducks.get(myId);
                                if (mine != null) {
                                    overlay.setLocalPosition(mine.x, mine.y, false);
                                }
                            }
                            overlay.updateWorld(ws.ducks);
                        }
                    }

                    @Override
                    public void connected(Connection c) {
                        connected = true;
                        System.out.println("✅ Connected to DuckBongo server!");
                        // Do NOT send initial position here. The server already spawned us
                        // with a randomized position and broadcasted a snapshot. We'll sync
                        // our local position to the server snapshot when it arrives.
                    }

                    @Override
                    public void disconnected(Connection c) {
                        connected = false;
                        // Optional: clear myId so we don't ignore wrong entries
                        Platform.runLater(() -> overlay.setMyId(-1));
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
