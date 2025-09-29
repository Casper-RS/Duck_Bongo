package dev.casperrs.duckbongo;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.Result;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.activity.ActivityParty;
import de.jcm.discordgamesdk.activity.ActivityJoinRequestReply;
import de.jcm.discordgamesdk.activity.ActivitySecrets;
import de.jcm.discordgamesdk.activity.ActivityType;
import dev.casperrs.duckbongo.core.PointsManager;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

public class ActivityExample {

    private static Core core;
    private static Instant startTime;
    private static String lobbyId = "";
    private static boolean inLobby = false;
    private static int currentPlayers = 1;
    private static int maxPlayers = 4;
    private static Consumer<String> joinRequestCallback;

    /**
     * Starts the Discord Core and sets an initial activity.
     */
    public static void runActivityHook(PointsManager points) throws IOException {
        try (CreateParams params = new CreateParams()) {
            params.setClientID(1418931416724406292L);
            params.setFlags(CreateParams.getDefaultFlags());
            
            core = new Core(params);
            startTime = Instant.now();

            // Note: The Discord Game SDK for Java doesn't directly expose join request events.
            // Join requests will be handled through the Discord client's UI directly.
            // The sendRequestReply method is available to respond to join requests.
            System.out.println("Discord activity manager initialized. Join requests will be handled through Discord's UI.");

            // Initial activity
            updateLobbyActivity(false);

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
     * Creates or updates the lobby activity
     */
    public static void updateLobbyActivity(boolean isHost) {
        if (core == null) return;

        try (Activity activity = new Activity()) {
            activity.setType(ActivityType.PLAYING);
            activity.timestamps().setStart(startTime);
            
            if (inLobby) {
                activity.setDetails("In Lobby");
                activity.setState("Players: " + currentPlayers + "/" + maxPlayers);
                
                // Set up party information
                ActivityParty party = activity.party();
                party.size().setCurrentSize(currentPlayers);
                party.size().setMaxSize(maxPlayers);
                // Set party ID through the activity's party ID field
                activity.party().setID("party_" + lobbyId);
                
                // Set up join secret
                ActivitySecrets secrets = activity.secrets();
                secrets.setJoinSecret("join_" + lobbyId);
                
                // Set up buttons
                activity.assets().setLargeImage("duck_icon");
                activity.assets().setLargeText("Duck Bongo");
                
                if (isHost) {
                    // Host can set match secret for spectate/join
                    secrets.setMatchSecret("match_" + lobbyId);
                }
            } else {
                activity.setDetails("Bongin' Ducks");
                activity.setState("In Menu");
                activity.assets().setLargeImage("duck_icon");
                activity.assets().setLargeText("Duck Bongo");
            }
            
            core.activityManager().updateActivity(activity);
        }
    }

    /**
     * Creates a new lobby
     */
    public static String createLobby(int maxPlayers, Consumer<String> onJoinRequest) {
        if (core == null) return null;
        inLobby = true;
        currentPlayers = 1;
        ActivityExample.maxPlayers = maxPlayers;
        lobbyId = UUID.randomUUID().toString().substring(0, 6).toUpperCase(); // Shorter, more readable ID
        joinRequestCallback = onJoinRequest;
        
        // Set up the activity with proper permissions
        try (Activity activity = new Activity()) {
            activity.setDetails("Playing Duck Bongo");
            activity.setState("In Lobby - " + currentPlayers + "/" + maxPlayers);
            activity.setType(ActivityType.PLAYING);
            
            // Set up party information
            ActivityParty party = activity.party();
            party.size().setCurrentSize(currentPlayers);
            party.size().setMaxSize(maxPlayers);
            party.setID("party_" + lobbyId);
            
            // Set up join secret - this is crucial for the "Ask to Join" button
            ActivitySecrets secrets = activity.secrets();
            String joinSecret = "join_" + lobbyId;
            secrets.setJoinSecret(joinSecret);
            
            // Set up match secret (required for join to work)
            String matchSecret = "match_" + lobbyId;
            secrets.setMatchSecret(matchSecret);
            
            // Set activity timestamps
            activity.timestamps().setStart(Instant.now());
            
            // Update the activity with a callback to verify it was successful
            core.activityManager().updateActivity(activity, result -> {
                if (result == Result.OK) {
                    System.out.println("Successfully updated activity with join secret: " + joinSecret);
                } else {
                    System.err.println("Failed to update activity: " + result);
                }
            });
        }
        
        System.out.println("Created lobby with ID: " + lobbyId);
        return lobbyId;
    }
    
    /**
     * Joins an existing lobby
     */
    public static void joinLobby(String lobbyId) {
        if (core == null) return;
        
        inLobby = true;
        ActivityExample.lobbyId = lobbyId;
        
        // Set up the activity for joining
        try (Activity activity = new Activity()) {
            activity.setDetails("Joining Duck Bongo Lobby");
            activity.setState("Connecting...");
            activity.setType(ActivityType.PLAYING);
            
            // Set up party information
            ActivityParty party = activity.party();
            party.setID("party_" + lobbyId);
            
            // Set up join secret
            ActivitySecrets secrets = activity.secrets();
            secrets.setJoinSecret("join_" + lobbyId);
            
            // Update the activity
            core.activityManager().updateActivity(activity, result -> {
                if (result == Result.OK) {
                    System.out.println("Successfully updated activity for joining lobby: " + lobbyId);
                } else {
                    System.err.println("Failed to update activity for joining: " + result);
                }
            });
        }
    }

    /**
     * Updates the player count in the lobby
     */
    public static void updatePlayerCount(int currentPlayers, int maxPlayers) {
        if (core == null || !inLobby) return;
        
        ActivityExample.currentPlayers = currentPlayers;
        ActivityExample.maxPlayers = maxPlayers;
        updateLobbyActivity(false);
    }

    /**
     * Leaves the current lobby
     */
    public static void leaveLobby() {
        inLobby = false;
        lobbyId = "";
        if (core != null) {
            updateLobbyActivity(false);
        }
    }

    /**
     * Responds to a join request
     */
    public static void respondToJoinRequest(String userId, boolean accept) {
        if (core != null) {
            try {
                long userIdLong = Long.parseLong(userId);
                core.activityManager().sendRequestReply(userIdLong, 
                    accept ? ActivityJoinRequestReply.YES : ActivityJoinRequestReply.NO,
                    result -> {
                        if (result == Result.OK) {
                            if (accept) {
                                currentPlayers = Math.min(currentPlayers + 1, maxPlayers);
                                updateLobbyActivity(true);
                            }
                        } else {
                            System.err.println("Failed to respond to join request: " + result);
                        }
                    });
            } catch (NumberFormatException e) {
                System.err.println("Invalid user ID format: " + userId);
            }
        }
    }

    /**
     * Updates the activity with custom details and state
     */
    public static void updateActivity(String details, String state) {
        if (core != null && !inLobby) {
            try (Activity activity = new Activity()) {
                activity.setDetails(details);
                activity.setState(state);
                activity.timestamps().setStart(startTime);
                activity.assets().setLargeImage("duck_icon");
                activity.assets().setLargeText("Duck Bongo");
                core.activityManager().updateActivity(activity);
            }
        }
    }
}

