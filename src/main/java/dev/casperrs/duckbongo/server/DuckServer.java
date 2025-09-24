package dev.casperrs.duckbongo.server;

import java.io.IOException;
import java.util.HashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import dev.casperrs.duckbongo.network.DuckState;
import dev.casperrs.duckbongo.network.WorldState;

/**
 * Simple multiplayer server for DuckBongo.
 * Tracks each player's duck position/skin and
 * broadcasts the full world state to every client.
 */
//public class DuckServer {
//
//    /** Map of connection ID → latest duck state */
//    private final HashMap<Integer, DuckState> ducks = new HashMap<>();
//
//    public DuckServer() throws IOException {
//        Server server = new Server();
//        Kryo kryo = server.getKryo();
//
//        // Register the classes to send across the network
//        kryo.register(DuckState.class);
//        kryo.register(WorldState.class);
//        kryo.register(HashMap.class);
//
//        // Listen for client messages
//        server.addListener(new Listener() {
//            @Override
//            public void received(Connection c, Object obj) {
//                if (obj instanceof DuckState) {
//                    // Update this player's duck and broadcast new world state
//                    ducks.put(c.getID(), (DuckState) obj);
//
//                    WorldState state = new WorldState();
//                    state.ducks = new HashMap<>(ducks); // copy for safety
//                    server.sendToAllTCP(state);
//                }
//            }
//
//            @Override
//            public void disconnected(Connection c) {
//                // Remove duck when a client disconnects
//                ducks.remove(c.getID());
//                WorldState state = new WorldState();
//                state.ducks = new HashMap<>(ducks);
//                server.sendToAllTCP(state);
//            }
//        });
//
//        // Bind and start server (TCP port 54555, UDP port 54777)
//        server.bind(54555, 54777);
//        server.start();
//
//        System.out.println("DuckServer running on ports 54555 (TCP) / 54777 (UDP)");
//    }
//
//    public static void main(String[] args) throws IOException {
//        new DuckServer();
//    }
//}

//public class DuckServer {
//    private final HashMap<Integer, DuckState> ducks = new HashMap<>();
//    private final Server server = new Server();
//
//    public DuckServer() throws IOException {
//        Kryo kryo = server.getKryo();
//        kryo.register(DuckState.class);
//        kryo.register(WorldState.class);
//        kryo.register(HashMap.class);
//
//        server.addListener(new Listener() {
//            @Override
//            public void received(Connection c, Object obj) {
//                if (obj instanceof DuckState state) {
//                    // Save/update this client's duck
//                    ducks.put(c.getID(), state);
//                    // Broadcast to all clients
//                    server.sendToAllTCP(new WorldState(new HashMap<>(ducks)));
//                }
//            }
//
//            @Override
//            public void disconnected(Connection c) {
//                ducks.remove(c.getID());
//                server.sendToAllTCP(new WorldState(new HashMap<>(ducks)));
//            }
//        });
//
//        server.bind(54555, 54777);
//        server.start();
//        System.out.println("DuckServer started!");
//    }
//
//    public static void main(String[] args) throws IOException {
//        new DuckServer();
//    }
//}

public class DuckServer {

    private final HashMap<Integer, DuckState> ducks = new HashMap<>();
    private final Server server = new Server();

    public DuckServer() throws IOException {
        Kryo kryo = server.getKryo();
        kryo.register(DuckState.class);
        kryo.register(WorldState.class);
        kryo.register(HashMap.class);

        server.addListener(new Listener() {
            @Override
            public void received(Connection c, Object obj) {
                if (obj instanceof DuckState state) {
                    ducks.put(c.getID(), state); // update this player's duck
                    server.sendToAllTCP(new WorldState(new HashMap<>(ducks))); // broadcast
                }
            }

            @Override
            public void disconnected(Connection c) {
                ducks.remove(c.getID());
                server.sendToAllTCP(new WorldState(new HashMap<>(ducks)));
            }
        });

        server.bind(54555, 54777);
        server.start();
        System.out.println("DuckServer started on ports 54555/54777");
    }

    public static void main(String[] args) throws IOException {
        new DuckServer();
    }
}


