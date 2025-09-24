//package dev.casperrs.duckbongo.network;
//
//import java.util.HashMap;
//
//// Packet sent by the server
//public class WorldState {
//    public HashMap<Integer, DuckState> ducks;
//
//    public WorldState() {} // no-arg constructor for Kryo
//}

package dev.casperrs.duckbongo.network;

import java.util.HashMap;

public class WorldState {
    public HashMap<Integer, DuckState> ducks;

    // No-arg constructor for Kryo
    public WorldState() {}

    // Constructor that sets the ducks map
    public WorldState(HashMap<Integer, DuckState> ducks) {
        this.ducks = ducks;
    }
}