package dev.casperrs.duckbongo.network;

import java.util.HashMap;

// Packet sent by the server
public class WorldState {
    public HashMap<Integer, DuckState> ducks;

    public WorldState() {} // no-arg constructor for Kryo
}
