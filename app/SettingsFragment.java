package com.example.location;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class SettingsFragment extends Fragment {

    private Switch notificationSwitch, darkModeSwitch;
    private Button backupButton, feedbackButton;
    private SharedPreferences sharedPreferences;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        notificationSwitch = view.findViewById(R.id.notificationSwitch);
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        backupButton = view.findViewById(R.id.backupButton);
        feedbackButton = view.findViewById(R.id.feedbackButton);
        sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadPreferences();

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveBooleanPreference("notifications_enabled", isChecked);
            Toast.makeText(getContext(), isChecked ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
        });

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveBooleanPreference("dark_mode_enabled", isChecked);
            Toast.makeText(getContext(), isChecked ? "Dark mode enabled" : "Dark mode disabled", Toast.LENGTH_SHORT).show();
            applyDarkMode(isChecked);
        });

        backupButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Backing up notes...", Toast.LENGTH_SHORT).show();
            backupNotes();
        });

        feedbackButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FeedbackActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void loadPreferences() {
        boolean notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true);
        boolean darkModeEnabled = sharedPreferences.getBoolean("dark_mode_enabled", false);

        notificationSwitch.setChecked(notificationsEnabled);
        darkModeSwitch.setChecked(darkModeEnabled);
    }

    private void saveBooleanPreference(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void applyDarkMode(boolean isDarkModeEnabled) {
        if (isDarkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void backupNotes() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            db.collection("users").document(userId).collection("notes")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                db.collection("users").document(userId).collection("backup_notes")
                                        .document(document.getId())
                                        .set(document.getData())
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), "Backup successful", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Backup failed", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to fetch notes for backup", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}