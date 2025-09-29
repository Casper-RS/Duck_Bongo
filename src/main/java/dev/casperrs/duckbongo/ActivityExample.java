package dev.casperrs.duckbongo;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import dev.casperrs.duckbongo.core.PointsManager;

import java.io.IOException;
import java.time.Instant;

public class ActivityExample {

    private static Core core; // static so MainApp can update activity
    private static Instant startTime; // store original start time
    private static int playerCount = 0; // track number of players in the server

    /**
     * Starts the Discord Core and sets an initial activity.
     */
    public static void runActivityHook(PointsManager points) throws IOException {
        try (CreateParams params = new CreateParams()) {
            params.setClientID(1418931416724406292L); // replace with your client ID
            params.setFlags(CreateParams.getDefaultFlags());

            core = new Core(params);
            startTime = Instant.now();

            // Initial activity
            try (Activity activity = new Activity()) {
                activity.setDetails("bongin ducks");
                activity.setState("and having fun");
                activity.timestamps().setStart(startTime);
                activity.assets().setLargeImage("test");
                activity.party().setID("Party!");
                activity.secrets().setJoinSecret("Join!");
                core.activityManager().updateActivity(activity);
            }

            // Run the callbacks loop forever
            new Thread(() -> {
                while (true) {
                    core.runCallbacks();
                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, "Discord-RPC-Callbacks").start();
        }
    }

    /**
     * Static helper for updating the activity from elsewhere (e.g., MainApp).
     */
    /**
     * Updates the activity with new details and state
     * @param details Main activity text (first line)
     * @param state Secondary activity text (second line)
     */
    public static void updateActivity(String details, String state) {
        updateActivity(details, state, playerCount);
    }

    /**
     * Updates the activity with new details, state, and player count
     * @param details Main activity text (first line)
     * @param state Secondary activity text (second line)
     * @param players Number of players in the server (0 to hide)
     */
    public static void updateActivity(String details, String state, int players) {
        if (core != null) {
            try (Activity activity = new Activity()) {
                activity.setDetails(details);
                
                // Format the state to include player count if there are players
                String formattedState = state;
                if (players > 0) {
                    formattedState = String.format("%s • %d %s online", 
                        state, players, players == 1 ? "player" : "players");
                }
                activity.setState(formattedState);
                activity.timestamps().setStart(startTime); // reuse original start
                core.activityManager().updateActivity(activity);
            }
        }
    }
    
    /**
     * Updates just the player count in the activity
     * @param count Number of players in the server
     */
    public static void updatePlayerCount(int count) {
        playerCount = count;
        // Update the activity with the current state and new player count
        if (core != null) {
            try (Activity activity = new Activity()) {
                // Get the current activity details and state
                String currentDetails = "bongin ducks"; // Default details
                String currentState = "and having fun"; // Default state
                
                // Create a new activity with the updated player count
                activity.setDetails(currentDetails);
                String formattedState = currentState;
                if (count > 0) {
                    formattedState = String.format("%s • %d %s online", 
                        currentState, count, count == 1 ? "player" : "players");
                }
                activity.setState(formattedState);
                activity.timestamps().setStart(startTime);
                core.activityManager().updateActivity(activity);
            }
        }
    }
}

