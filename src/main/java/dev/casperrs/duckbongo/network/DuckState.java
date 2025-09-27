package dev.casperrs.duckbongo.network;



public class DuckState {
    public float x;
    public float y;
    public String skin;
    public String water;
    public String username;
    // If true, this packet represents a final resting pose and should be broadcast immediately
    public boolean settled;
    public DuckState() {}
}

