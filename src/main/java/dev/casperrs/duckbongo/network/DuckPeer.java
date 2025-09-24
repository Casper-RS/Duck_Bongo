package dev.casperrs.duckbongo.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.HashMap;

public class DuckPeer {

    private final HashMap<Integer, DuckState> ducks = new HashMap<>();
    private final Server server;
    private final Client client;

    public DuckPeer(int serverPort, String[] peerIPs, int peerPort) throws IOException {
        server = new Server();
        client = new Client();

        Kryo serverKryo = server.getKryo();
        Kryo clientKryo = client.getKryo();
        serverKryo.register(DuckState.class);
        serverKryo.register(WorldState.class);
        serverKryo.register(HashMap.class);
        clientKryo.register(DuckState.class);
        clientKryo.register(WorldState.class);
        clientKryo.register(HashMap.class);

        // Server listener
        server.addListener(new Listener() {
            @Override
            public void received(Connection c, Object obj) {
                if (obj instanceof DuckState state) {
                    ducks.put(c.getID(), state);
                    server.sendToAllTCP(new WorldState(new HashMap<>(ducks)));
                }
            }

            @Override
            public void disconnected(Connection c) {
                ducks.remove(c.getID());
                server.sendToAllTCP(new WorldState(new HashMap<>(ducks)));
            }
        });

        server.bind(serverPort);
        server.start();

        client.start();
        if (peerIPs != null) {
            for (String ip : peerIPs) {
                if (ip != null && !ip.isEmpty()) client.connect(5000, ip, peerPort);
            }
        }

        client.addListener(new Listener() {
            @Override
            public void received(Connection c, Object obj) {
                if (obj instanceof WorldState ws) {
                    synchronized (ducks) {
                        ducks.clear();
                        ducks.putAll(ws.ducks);
                    }
                }
            }
        });
    }

    // Send your duck to other peers
    public void sendMyDuck(DuckState me) {
        client.sendTCP(me);
        synchronized (ducks) {
            ducks.put(-1, me); // optional: store own duck with key -1
        }
    }

    // <-- THIS IS THE METHOD YOU NEED -->
    public HashMap<Integer, DuckState> getWorld() {
        synchronized (ducks) {
            return new HashMap<>(ducks);
        }
    }
}
