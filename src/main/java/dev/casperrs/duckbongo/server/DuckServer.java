package dev.casperrs.duckbongo.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.*;
import dev.casperrs.duckbongo.network.DuckState;
import dev.casperrs.duckbongo.network.WorldState;
import dev.casperrs.duckbongo.network.AssignId;
import dev.casperrs.duckbongo.network.MoveOther;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public class DuckServer {
    private final Server server = new Server(16384, 4096);
    private final HashMap<Integer, DuckState> world = new HashMap<>();
    private final Random rng = new Random();

    // Throttle world snapshots to avoid spamming (about 20 Hz)
    private static final long BROADCAST_INTERVAL_MS = 50;
    private long lastBroadcastMs = 0;

    public void start(int tcpPort, int udpPort) throws IOException {
        Kryo kryo = server.getKryo();

        kryo.register(HashMap.class);
        kryo.register(DuckState.class);
        kryo.register(WorldState.class);
        kryo.register(AssignId.class);
        kryo.register(MoveOther.class);

        server.addListener(new Listener() {
            @Override public void connected(Connection c) {
                DuckState s = new DuckState();
                // Randomize initial spawn so new ducks aren't stacked
                s.x = 60 + rng.nextInt(840); // ~60..900
                s.y = 60 + rng.nextInt(540); // ~60..600
                s.skin = "/assets/skin_parts/ducks/duck_default.png";
                s.water = "/assets/skin_parts/waters/water_default.png";
                world.put(c.getID(), s);

                // Tell the client the server-assigned ID
                server.sendToTCP(c.getID(), new AssignId(c.getID()));

                broadcastSnapshot(true); // force immediate broadcast on new connection
            }

            @Override public void disconnected(Connection c) {
                world.remove(c.getID());
                broadcastSnapshot(false);
            }

            @Override public void received(Connection c, Object obj) {
                if (obj instanceof DuckState s) {
                    DuckState mine = world.computeIfAbsent(c.getID(), k -> new DuckState());
                    mine.x = s.x;
                    mine.y = s.y;
                    mine.skin = s.skin;
                    mine.water = s.water;
                    if (s.settled) {
                        broadcastSnapshot(true); // final pose: send immediately
                    } else {
                        broadcastSnapshot(false);
                    }
                } else if (obj instanceof MoveOther mo) {
                    DuckState target = world.get(mo.targetId);
                    if (target != null) {
                        target.x = mo.x;
                        target.y = mo.y;
                        if (mo.settled) {
                            broadcastSnapshot(true);
                        } else {
                            broadcastSnapshot(false);
                        }
                    }
                }
            }
        });

        server.bind(tcpPort, udpPort);
        server.start();
        System.out.println("DuckServer running on TCP " + tcpPort + ", UDP " + udpPort);
    }

    private void broadcastSnapshot(boolean force) {
        long now = System.currentTimeMillis();
        if (!force) {
            if (now - lastBroadcastMs < BROADCAST_INTERVAL_MS) return;
        }
        lastBroadcastMs = now;
        // send a deep copy so clients don’t mutate our map and entries aren't sharing instances
        HashMap<Integer, DuckState> copy = new HashMap<>();
        for (var e : world.entrySet()) {
            DuckState s = e.getValue();
            DuckState cpy = new DuckState();
            cpy.x = s.x; cpy.y = s.y; cpy.skin = s.skin; cpy.water = s.water;
            copy.put(e.getKey(), cpy);
        }
        WorldState ws = new WorldState(copy);
        server.sendToAllUDP(ws);
    }

    public static void main(String[] args) throws IOException {
        new DuckServer().start(54555, 54777);
    }
}
