package com.example.location;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "note_reminder_channel";

    public static void showNotification(Context context, String title, String message) {
        // Check if notification permission is granted (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, return without showing the notification
                return;
            }
        }

        // Intent to open the app's home screen when notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Sound and vibration pattern
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        long[] vibrationPattern = {0, 500, 1000}; // Wait 0ms, vibrate 500ms, wait 1000ms

        // Create Notification Channel for Android 8+ (Oreo+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Note Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Channel for note reminders");
            channel.enableVibration(true);
            channel.setVibrationPattern(vibrationPattern);
            channel.setSound(soundUri, null);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Notification icon
                .setContentTitle(title) // Use the note's title
                .setContentText(message) // Use the note's content
                .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for instant alert
                .setAutoCancel(true) // Automatically dismiss the notification when tapped
                .setSound(soundUri) // Set notification sound
                .setVibrate(vibrationPattern) // Set vibration pattern
                .setContentIntent(pendingIntent); // Open the app's home screen on tap

        // Show notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build()); // Unique ID for each notification
    }
}