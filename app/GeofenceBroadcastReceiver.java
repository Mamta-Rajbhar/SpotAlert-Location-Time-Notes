package com.example.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Geofence error: " + geofencingEvent.getErrorCode());
            return;
        }

        int transitionType = geofencingEvent.getGeofenceTransition();
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            for (Geofence geofence : triggeringGeofences) {
                String noteId = geofence.getRequestId();
                triggerLocationNotification(context, noteId);
            }
        }
    }

    private void triggerLocationNotification(Context context, String noteId) {
        // Fetch the note from Firestore using noteId and trigger a notification
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).collection("notes").document(noteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> note = documentSnapshot.getData();
                        String title = note.get("title").toString();
                        String content = note.get("content").toString();
                        NotificationHelper.showNotification(context, title, content);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("GeofenceReceiver", "Failed to fetch note: " + e.getMessage());
                });
    }
}