package com.example.location;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GeofenceHelper {

    private static final String PREFS_NAME = "GeofencePrefs";
    private static final String KEY_GEOFENCE_IDS = "geofence_ids";
    private static final int MAX_GEOFENCES = 100;  // Android's geofence limit is 100 per app

    private Context context;
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;
    private SharedPreferences sharedPreferences;

    // Constructor
    public GeofenceHelper(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Add geofence with limit management and duplicate check
    public void addGeofence(String geofenceId, double latitude, double longitude, float radius) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("GeofenceHelper", "Location permission not granted");
            return;
        }

        // Check for duplicates
        Set<String> geofenceIds = getGeofenceIds();
        if (geofenceIds.contains(geofenceId)) {
            Log.w("GeofenceHelper", "Geofence already exists: " + geofenceId);
            return;  // Prevent duplicate geofence
        }

        // Check geofence limit
        if (geofenceIds.size() >= MAX_GEOFENCES) {
            Log.w("GeofenceHelper", "Geofence limit reached. Removing old geofences...");
            removeOldGeofences();
        }

        // Build geofence
        Geofence geofence = new Geofence.Builder()
                .setRequestId(geofenceId)
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        // Build geofencing request
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build();

        // Add geofence with error handling
        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener(aVoid -> {
                    Log.d("GeofenceHelper", "Geofence added successfully: " + geofenceId);
                    saveGeofenceId(geofenceId);  // Save the geofence ID
                })
                .addOnFailureListener(e -> {
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                        Log.e("GeofenceHelper", "Error Code: " + apiException.getStatusCode());
                        handleApiException(apiException);
                    } else {
                        Log.e("GeofenceHelper", "Unknown error: " + e.getMessage());
                    }
                });
    }

    // Handle common geofence exceptions
    private void handleApiException(ApiException e) {
        switch (e.getStatusCode()) {
            case 1004:  // Geofence limit reached
                Log.e("GeofenceHelper", "Geofence limit reached. Removing old geofences...");
                removeOldGeofences();
                break;
            case 1024:  // Network issues
                Log.e("GeofenceHelper", "Network error while adding geofence.");
                break;
            default:
                Log.e("GeofenceHelper", "Unexpected error: " + e.getStatusCode());
        }
    }

    // Remove old geofences automatically
    private void removeOldGeofences() {
        Set<String> geofenceIds = getGeofenceIds();

        if (!geofenceIds.isEmpty()) {
            ArrayList<String> idsToRemove = new ArrayList<>(geofenceIds);

            geofencingClient.removeGeofences(idsToRemove)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("GeofenceHelper", "Old geofences removed.");
                        clearGeofenceIds();  // Clear the saved geofence IDs
                    })
                    .addOnFailureListener(e -> Log.e("GeofenceHelper", "Failed to remove old geofences: " + e.getMessage()));
        }
    }

    // Clear all geofences on app launch
    public void clearAllGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(aVoid -> {
                    Log.d("GeofenceHelper", "All geofences cleared.");
                    clearGeofenceIds();  // Clear the saved geofence IDs
                })
                .addOnFailureListener(e -> Log.e("GeofenceHelper", "Failed to clear all geofences: " + e.getMessage()));
    }

    // Remove a single geofence by its ID
    public void removeGeofence(String geofenceId) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("GeofenceHelper", "Location permission not granted");
            return;
        }

        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(geofenceId);

        geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener(aVoid -> {
                    Log.d("GeofenceHelper", "Geofence removed: " + geofenceId);
                    removeGeofenceId(geofenceId); // Remove the geofence ID from SharedPreferences
                })
                .addOnFailureListener(e -> {
                    Log.e("GeofenceHelper", "Failed to remove geofence: " + geofenceId, e);
                });
    }

    // Remove a geofence ID from SharedPreferences
    private void removeGeofenceId(String geofenceId) {
        Set<String> geofenceIds = getGeofenceIds();
        geofenceIds.remove(geofenceId);
        sharedPreferences.edit().putStringSet(KEY_GEOFENCE_IDS, geofenceIds).apply();
    }

    // Get or create geofence pending intent with null safety
    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;  // For Android 12+
        }

        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
        return geofencePendingIntent;
    }

    // Manage geofence IDs in SharedPreferences
    private Set<String> getGeofenceIds() {
        return sharedPreferences.getStringSet(KEY_GEOFENCE_IDS, new HashSet<>());
    }

    private void saveGeofenceId(String geofenceId) {
        Set<String> geofenceIds = getGeofenceIds();
        geofenceIds.add(geofenceId);
        sharedPreferences.edit().putStringSet(KEY_GEOFENCE_IDS, geofenceIds).apply();
    }

    private void clearGeofenceIds() {
        sharedPreferences.edit().remove(KEY_GEOFENCE_IDS).apply();
    }
}