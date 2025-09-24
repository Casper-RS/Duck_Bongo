package dev.casperrs.duckbongo.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.HashMap;

public class DuckPeer {

    private final Server server;
    private final HashMap<Integer, DuckState> world = new HashMap<>();

    public DuckPeer(int port, String[] peerIPs, int peerPort) throws IOException {
        server = new Server();
        server.start();

        Kryo kryo = server.getKryo();
        kryo.register(DuckState.class);
        kryo.register(HashMap.class);

        server.addListener(new Listener() {
            @Override
            public void received(Connection c, Object obj) {
                if (obj instanceof DuckState state) {
                    synchronized (world) {
                        world.put(c.getID(), state);
                    }
                }
            }

            @Override
            public void disconnected(Connection c) {
                synchronized (world) {
                    world.remove(c.getID());
                }
            }
        });

        server.bind(port);

        // Connect to other peers
        for (String ip : peerIPs) {
            try {
                com.esotericsoftware.kryonet.Client client = new com.esotericsoftware.kryonet.Client();
                client.start();
                Kryo clientKryo = client.getKryo();
                clientKryo.register(DuckState.class);
                clientKryo.register(HashMap.class);
                client.connect(5000, ip, peerPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("DuckPeer running on port " + port);
    }

    // Send your own duck state to all connected clients
    public void sendMyDuck(DuckState state) {
        for (Connection c : server.getConnections()) {
            c.sendTCP(state);
        }
    }

    // Return a copy of the world map for safe iteration
    public HashMap<Integer, DuckState> getWorld() {
        synchronized (world) {
            return new HashMap<>(world);
        }
    }

    public void stop() {
        server.stop();
    }
}
