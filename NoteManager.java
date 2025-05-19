package com.example.location;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class NoteManager {

    private static final String TAG = "NoteManager";

    // Delete a note and move it to the bin
    public static void deleteNote(Context context, String noteId, Map<String, Object> note, OnNoteOperationListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        // Remove the geofence associated with the note
        GeofenceHelper geofenceHelper = new GeofenceHelper(context);
        geofenceHelper.removeGeofence(noteId);

        db.collection("users").document(userId).collection("notes").document(noteId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(userId).collection("bin").document(noteId)
                            .set(note)
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "Note moved to bin: " + noteId);
                                listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to move note to bin: " + e.getMessage());
                                listener.onFailure("Failed to move note to bin");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete note: " + e.getMessage());
                    listener.onFailure("Failed to delete note");
                });
    }

    // Archive a note
    public static void archiveNote(Context context, String noteId, Map<String, Object> note, OnNoteOperationListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        // Remove the geofence associated with the note
        GeofenceHelper geofenceHelper = new GeofenceHelper(context);
        geofenceHelper.removeGeofence(noteId);

        db.collection("users").document(userId).collection("notes").document(noteId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(userId).collection("archive").document(noteId)
                            .set(note)
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "Note archived: " + noteId);
                                listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to archive note: " + e.getMessage());
                                listener.onFailure("Failed to archive note");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete note: " + e.getMessage());
                    listener.onFailure("Failed to delete note");
                });
    }

    // Callback interface for note operations
    public interface OnNoteOperationListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }
}