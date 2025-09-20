package dev.casperrs.duckbongo;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import dev.casperrs.duckbongo.core.PointsManager;

import java.io.IOException;
import java.time.Instant;

public class ActivityExample {

    private static Core core; // static so MainApp can update activity

    /**
     * Starts the Discord Core and sets an initial activity.
     */
    public static void runActivityHook(PointsManager points) throws IOException {
        try (CreateParams params = new CreateParams()) {
            params.setClientID(1418931416724406292L); // replace with your client ID
            params.setFlags(CreateParams.getDefaultFlags());

            core = new Core(params);

            // Initial activity
            try (Activity activity = new Activity()) {
                activity.setDetails("bongin ducks");
                activity.setState("and having fun");
                activity.timestamps().setStart(Instant.now());
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
    public static void updateActivity(String details, String state) {
        if (core != null) {
            try (Activity activity = new Activity()) {
                activity.setDetails(details);
                activity.setState(state);
                activity.timestamps().setStart(Instant.now());
                core.activityManager().updateActivity(activity);
            }
        }
    }
}

