package dev.casperrs.duckbongo.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.*;
import dev.casperrs.duckbongo.network.DuckState;
import dev.casperrs.duckbongo.network.WorldState;
import dev.casperrs.duckbongo.network.AssignId;

import java.io.IOException;
import java.util.HashMap;

public class DuckServer {
    private final Server server = new Server(16384, 4096);
    private final HashMap<Integer, DuckState> world = new HashMap<>();

    public void start(int tcpPort, int udpPort) throws IOException {
        Kryo kryo = server.getKryo();

        kryo.register(HashMap.class);
        kryo.register(DuckState.class);
        kryo.register(WorldState.class);
        kryo.register(AssignId.class);

        server.addListener(new Listener() {
            @Override public void connected(Connection c) {
                DuckState s = new DuckState();
                s.x = 50; s.y = 50;
                s.skin = "/assets/skin_parts/ducks/duck_default.png";
                s.water = "/assets/skin_parts/waters/water_default.png";
                world.put(c.getID(), s);

                // Tell the client the server-assigned ID
                server.sendToTCP(c.getID(), new AssignId(c.getID()));

                broadcastSnapshot();
            }

            @Override public void disconnected(Connection c) {
                world.remove(c.getID());
                broadcastSnapshot();
            }

            @Override public void received(Connection c, Object obj) {
                if (obj instanceof DuckState s) {
                    DuckState mine = world.computeIfAbsent(c.getID(), k -> new DuckState());
                    mine.x = s.x;
                    mine.y = s.y;
                    mine.skin = s.skin;
                    mine.water = s.water;
                    broadcastSnapshot();
                }
            }
        });

        server.bind(tcpPort, udpPort);
        server.start();
        System.out.println("DuckServer running on TCP " + tcpPort + ", UDP " + udpPort);
    }

    private void broadcastSnapshot() {
        // send a *copy* so clients don’t mutate our map
        WorldState ws = new WorldState(new HashMap<>(world));
        server.sendToAllTCP(ws); // switch to UDP if you prefer; TCP is fine for low rate
    }

    public static void main(String[] args) throws IOException {
        new DuckServer().start(54555, 54777);
    }
}
