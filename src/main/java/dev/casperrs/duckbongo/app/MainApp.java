////package dev.casperrs.duckbongo.app;
////
////
////import com.github.kwhat.jnativehook.NativeHookException;
////import de.jcm.discordgamesdk.activity.Activity;
////import dev.casperrs.duckbongo.ActivityExample;
////import dev.casperrs.duckbongo.ActivityUpdater;
////import dev.casperrs.duckbongo.core.PointsManager;
////import dev.casperrs.duckbongo.dataHandler.DataHandler;
////import dev.casperrs.duckbongo.input.InputHook;
////import dev.casperrs.duckbongo.network.DuckState;
////import dev.casperrs.duckbongo.network.WorldState;
////import javafx.animation.AnimationTimer;
////import javafx.application.Application;
////import javafx.stage.Stage;
////
////import java.time.Instant;
////import java.util.HashMap;
////
////// KryoNet
////import com.esotericsoftware.kryo.Kryo;
////import com.esotericsoftware.kryonet.Client;
////import com.esotericsoftware.kryonet.Connection;
////import com.esotericsoftware.kryonet.Listener;
////
////// JavaFX
////import javafx.application.Platform;
////
////// Java utilities
////import java.util.HashMap;
////
////public class MainApp extends Application {
////    private DuckOverlay overlay;
////    private long lastPointsSeen = 0;
////
////    private final PointsManager points = new PointsManager();
////    private DataHandler dataHandler = new DataHandler(points);
//
////    @Override
////    public void start(Stage stage) {
////        stage.setTitle("Duck Bongo");
////
////        // Load saved points
////        dataHandler.initAndLoad();
////
////        // Setup overlay
////        overlay = new DuckOverlay(stage, points);
////        overlay.show();
////
////        // ActivityUpdater: updates Discord activity
////        ActivityUpdater updater = (details, state) -> {
////            ActivityExample.updateActivity(details, state);
////        };
////
////        // Start input hooks
////        try {
////            InputHook hook = new InputHook(points, updater);
////            hook.start();
////        } catch (NativeHookException e) {
////            e.printStackTrace();
////        }
////
////        // Start Discord RPC in a separate thread (creates core + initial activity)
////        new Thread(() -> {
////            try {
////                ActivityExample.runActivityHook(points);
////            } catch (Exception e) {
////                e.printStackTrace();
////            }
////        }, "Discord-RPC").start();
////
////        // Update overlay only when points change
////        new AnimationTimer() {
////            @Override
////            public void handle(long now) {
////                long currentPoints = points.get();
////                if (currentPoints != lastPointsSeen) {
////                    overlay.punch(); // animates duck + updates counter
////                    lastPointsSeen = currentPoints;
////                }
////            }
////        }.start();
////    }
//
////    package dev.casperrs.duckbongo.app;
////
////import dev.casperrs.duckbongo.core.PointsManager;
////import dev.casperrs.duckbongo.dataHandler.DataHandler;
////import dev.casperrs.duckbongo.network.DuckState;
////import dev.casperrs.duckbongo.network.WorldState;
////
////import javafx.application.Application;
////import javafx.application.Platform;
////import javafx.stage.Stage;
////import javafx.animation.AnimationTimer;
////
////import com.esotericsoftware.kryo.Kryo;
////import com.esotericsoftware.kryonet.Client;
////import com.esotericsoftware.kryonet.Connection;
////import com.esotericsoftware.kryonet.Listener;
////
////import dev.casperrs.duckbongo.app.DuckOverlay;
////
////import java.util.HashMap;
////
////    public class MainApp extends Application {
////
////        private DuckOverlay overlay;
////        private long lastPointsSeen = 0;
////
////        private final PointsManager points = new PointsManager();
////        private DataHandler dataHandler = new DataHandler(points);
////
////        @Override
////        public void start(Stage stage) {
////
////            // Setup overlay
////            overlay = new DuckOverlay(stage, points);
////            overlay.show();
////
////            // Start client networking
////            startNetworking();
////
////            // Update overlay only when points change
////            new AnimationTimer() {
////                @Override
////                public void handle(long now) {
////                    long currentPoints = points.get();
////                    if (currentPoints != lastPointsSeen) {
////                        overlay.punch(); // animates duck + updates counter
////                        lastPointsSeen = currentPoints;
////                    }
////                }
////            }.start();
////        }
////
////        private void startNetworking() {
////            new Thread(() -> {
////                try {
////                    Client client = new Client();
////                    client.start();
////                    client.connect(5000, "localhost", 54555, 54777); // replace localhost with server IP
////
////                    Kryo kryo = client.getKryo();
////                    kryo.register(DuckState.class);
////                    kryo.register(WorldState.class);
////                    kryo.register(HashMap.class);
////
////                    // Listen for world updates
////                    client.addListener(new Listener() {
////                        @Override
////                        public void received(Connection c, Object obj) {
////                            if (obj instanceof WorldState ws) {
////                                Platform.runLater(() -> overlay.updateWorld(ws.ducks));
////                            }
////                        }
////                    });
////
////                    // Periodically send own duck state
////                    while (true) {
////                        DuckState me = new DuckState();
////                        me.x = overlay.getDuckX();
////                        me.y = overlay.getDuckY();
////                        me.skin = overlay.getDuckSkin();
////                        client.sendTCP(me);
////                        Thread.sleep(50); // 20 updates/sec
////                    }
////
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
////            }).start();
////        }
////
////        @Override
////        public void stop() {
////            if (dataHandler != null) {
////                dataHandler.save();
////            }
////        }
////
////        public static void main(String[] args) {
////            launch(args);
////        }
////    }
//
//package dev.casperrs.duckbongo.app;
//
//import dev.casperrs.duckbongo.core.PointsManager;
//import dev.casperrs.duckbongo.dataHandler.DataHandler;
//import dev.casperrs.duckbongo.network.DuckState;
//import dev.casperrs.duckbongo.network.WorldState;
//
//import javafx.animation.AnimationTimer;
//import javafx.application.Application;
//import javafx.application.Platform;
//import javafx.stage.Stage;
//
//import com.esotericsoftware.kryo.Kryo;
//import com.esotericsoftware.kryonet.Client;
//import com.esotericsoftware.kryonet.Connection;
//import com.esotericsoftware.kryonet.Listener;
//
//import java.util.HashMap;
//
//public class MainApp extends Application {
//
//    private DuckOverlay overlay;
//    private long lastPointsSeen = 0;
//
//    private final PointsManager points = new PointsManager();
//    private final DataHandler dataHandler = new DataHandler(points);
//
//    @Override
//    public void start(Stage stage) {
//        // Setup overlay
//        overlay = new DuckOverlay(stage, points);
//
//        // Start networking
//        startNetworking();
//
//        // Update overlay only when points change
//        new AnimationTimer() {
//            @Override
//            public void handle(long now) {
//                long currentPoints = points.get();
//                if (currentPoints != lastPointsSeen) {
//                    overlay.punch(); // animates duck + updates counter
//                    lastPointsSeen = currentPoints;
//                }
//            }
//        }.start();
//    }
//
//    private void startNetworking() {
//        new Thread(() -> {
//            try {
//                Client client = new Client();
//                client.start();
//
//                // Connect to server (replace "SERVER_IP" with the actual server IP)
//                client.connect(5000, "SERVER_IP", 54555, 54777);
//
//                // Register classes for Kryo serialization
//                Kryo kryo = client.getKryo();
//                kryo.register(DuckState.class);
//                kryo.register(WorldState.class);
//                kryo.register(HashMap.class);
//
//                // Listen for world updates from server
//                client.addListener(new Listener() {
//                    @Override
//                    public void received(Connection c, Object obj) {
//                        if (obj instanceof WorldState ws) {
//                            Platform.runLater(() -> overlay.updateWorld(ws.ducks));
//                        }
//                    }
//                });
//
//                // Periodically send this player's duck state
//                while (true) {
//                    DuckState me = new DuckState();
//                    me.x = overlay.getDuckX();
//                    me.y = overlay.getDuckY();
//                    me.skin = overlay.getDuckSkin();
//                    client.sendTCP(me);
//
//                    Thread.sleep(50); // 20 updates/sec
//                }
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }
//
//    @Override
//    public void stop() {
//        if (dataHandler != null) {
//            dataHandler.save();
//        }
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}
//
//

package dev.casperrs.duckbongo.app;

import dev.casperrs.duckbongo.core.PointsManager;
import dev.casperrs.duckbongo.dataHandler.DataHandler;
import dev.casperrs.duckbongo.network.DuckState;
import dev.casperrs.duckbongo.network.WorldState;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.util.HashMap;

public class MainApp extends Application {

    private DuckOverlay overlay;
    private long lastPointsSeen = 0;
    private final PointsManager points = new PointsManager();
    private final DataHandler dataHandler = new DataHandler(points);

    @Override
    public void start(Stage stage) {
        overlay = new DuckOverlay(stage, points);

        startNetworking();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                long currentPoints = points.get();
                if (currentPoints != lastPointsSeen) {
                    overlay.punch();
                    lastPointsSeen = currentPoints;
                }
            }
        }.start();
    }

    private void startNetworking() {
        new Thread(() -> {
            try {
                Client client = new Client();
                client.start();

                client.connect(5000, "localhost", 54555, 54777); // replace with server IP for other devices

                Kryo kryo = client.getKryo();
                kryo.register(DuckState.class);
                kryo.register(WorldState.class);
                kryo.register(HashMap.class);

                client.addListener(new Listener() {
                    @Override
                    public void received(Connection c, Object obj) {
                        if (obj instanceof WorldState ws) {
                            Platform.runLater(() -> overlay.updateWorld(ws.ducks));
                        }
                    }
                });

                while (true) {
                    DuckState me = new DuckState();
                    me.x = overlay.getDuckX();
                    me.y = overlay.getDuckY();
                    me.skin = overlay.getDuckSkin();
                    client.sendTCP(me);

                    Thread.sleep(50);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void stop() {
        if (dataHandler != null) dataHandler.save();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

