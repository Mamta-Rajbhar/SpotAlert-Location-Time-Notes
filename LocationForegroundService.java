package com.example.location;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LocationForegroundService extends Service {

    private static final String TAG = "LocationForegroundService";
    private static final String CHANNEL_ID = "location_service_channel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    // Track notified notes to prevent continuous notifications
    private Set<String> notifiedNotes = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationForegroundService onCreate() called");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        startLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Log.d(TAG, "LocationForegroundService onStartCommand() called");
        Notification notification = createNotification();
        startForeground(1, notification);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText("Tracking your location in the background")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Log.d(TAG, "Location update: " + latitude + ", " + longitude);

                    // Check proximity to saved notes
                    checkProximityToNotes(latitude, longitude);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void checkProximityToNotes(double latitude, double longitude) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).collection("notes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (document.contains("place")) {
                                Map<String, Object> place = (Map<String, Object>) document.get("place");
                                if (place != null && place.containsKey("latitude") && place.containsKey("longitude")) {
                                    double noteLatitude = (double) place.get("latitude");
                                    double noteLongitude = (double) place.get("longitude");

                                    float distance = calculateDistance(latitude, longitude, noteLatitude, noteLongitude);
                                    if (distance <= 100) { // 100 meters
                                        String noteId = document.getId();
                                        if (!notifiedNotes.contains(noteId)) {
                                            String title = document.getString("title");
                                            String content = document.getString("content");

                                            // Extract the location name from the note
                                            String locationName = "";
                                            if (place.containsKey("name") && place.get("name") != null) {
                                                locationName = place.get("name").toString();
                                            }

                                            // Create the notification message
                                            String notificationMessage = "You are near: " + locationName + "\nNote: " + content;

                                            // Show the notification
                                            NotificationHelper.showNotification(this, title, notificationMessage);
                                            // Mark this note as notified
                                            notifiedNotes.add(noteId);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Error fetching notes: " + task.getException());
                    }
                });
    }

    private float calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0]; // Distance in meters
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}