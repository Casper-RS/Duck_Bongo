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
public class DuckServer {

    private final HashMap<Integer, DuckState> ducks = new HashMap<>();
    private final Server server = new Server();

    public DuckServer() throws IOException {
        // Configure Kryo serialization
        Kryo kryo = server.getKryo();
        kryo.register(DuckState.class);
        kryo.register(WorldState.class);
        kryo.register(HashMap.class);

        // Handle client connections and messages
        server.addListener(new Listener() {
            // @Override
            // public void received(Connection c, Object obj) {
            //     if (obj instanceof DuckState state) {
            //         synchronized (ducks) {
            //             // Update this player's duck state
            //             ducks.put(c.getID(), state);
                        
            //             // Broadcast updated world state to all clients
            //             WorldState worldState = new WorldState(new HashMap<>(ducks));
            //             server.sendToAllTCP(worldState);
            //         }
                    
            //         System.out.println("Updated duck for client " + c.getID() + 
            //                          " at position (" + state.x + ", " + state.y + ")");
            //     }
            // }

            // @Override
            // public void received(Connection c, Object obj) {
            //     if (obj instanceof DuckState s) {
            //         // Store per-connection duck state
            //         ducks.put(c.getID(), s);
            //         // Immediately broadcast new world (or do this on a tick)
            //         WorldState ws = new WorldState();
            //         ws.ducks = new HashMap<>(ducks);
            //         server.sendToAllTCP(ws);
            //     }
            // }

            @Override
public void received(Connection c, Object obj) {
    if (obj instanceof DuckState s) {
        int id = c.getID();

        // Preserve last known state and only override fields that are provided
        DuckState prev = ducks.get(id);
        if (prev != null) {
            // Keep previous skin if incoming skin is null/blank
            if (s.skin == null || s.skin.isBlank()) {
                s.skin = prev.skin;
            }
            // Keep previous water if incoming water is null/blank
            if (s.water == null || s.water.isBlank()) {
                s.water = prev.water;
            }
            // If you want to preserve last X/Y when a client might send zeros, uncomment:
            // if (s.x == 0 && s.y == 0) {
            //     s.x = prev.x;
            //     s.y = prev.y;
            // }
        }

        ducks.put(id, s);

        // Debug: confirm incoming values
        System.out.println("[Server] recv id=" + id + " x=" + s.x + " y=" + s.y + " skin=" + s.skin);

        // Immediately broadcast new world (or do this on a tick)
        WorldState ws = new WorldState();
        ws.ducks = new HashMap<>(ducks);
        server.sendToAllTCP(ws);
    }
}

            @Override
            public void connected(Connection c) {
                System.out.println("Client " + c.getID() + " connected from " + c.getRemoteAddressTCP());
            }

            // @Override
            // public void disconnected(Connection c) {
            //     synchronized (ducks) {
            //         // Remove duck when client disconnects
            //         ducks.remove(c.getID());
                    
            //         // Broadcast updated world state
            //         WorldState worldState = new WorldState(new HashMap<>(ducks));
            //         server.sendToAllTCP(worldState);
            //     }
                
            //     System.out.println("Client " + c.getID() + " disconnected");
            // }

            @Override
            public void disconnected(Connection c) {
                ducks.remove(c.getID());
                WorldState ws = new WorldState();
                ws.ducks = new HashMap<>(ducks);
                server.sendToAllTCP(ws);
            }
        });

        // Bind to ports and start server
        server.bind(54555, 54777);
        server.start();
        
        System.out.println("🦆 DuckBongo Server started!");
        System.out.println("📡 Listening on TCP port 54555 and UDP port 54777");
        System.out.println("🎮 Waiting for players to connect...");
    }

    public void stop() {
        server.stop();
        System.out.println("Server stopped.");
    }

    public static void main(String[] args) {
        try {
            DuckServer server = new DuckServer();
            
            // Keep server running until interrupted
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            
            // Keep main thread alive
            while (true) {
                Thread.sleep(1000);
            }
            
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Server interrupted, shutting down...");
        }
    }
}


