package com.example.location;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView notesRecyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private GoogleSignInClient gClient;
    private GoogleSignInOptions gOptions;
    private FusedLocationProviderClient locationClient;
    private ImageView profileIcon;

    // Drawer and NavigationView
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;

    // Added empty state TextView as per Archive code
    private TextView emptyStateText;

    // RecyclerView Adapter and List for Notes
    private NoteAdapter noteAdapter; // Class-level variable
    private List<Map<String, Object>> notesList;
    private ListenerRegistration notesListener;

    // Location Callback for real-time location updates
    private LocationCallback locationCallback;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int GPS_REQUEST_CODE = 1002;

    private FusedLocationProviderClient fusedLocationClient;
    private boolean isFirstLaunch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        profileIcon = findViewById(R.id.profile_icon);

        // Initialize Drawer and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Initialize Icons
        ImageView menuIcon = findViewById(R.id.menu_icon); // Menu Icon (Drawer Toggle)
        EditText searchInput = findViewById(R.id.search_input); // Search Input

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter the notes based on the search query
                noteAdapter.getFilter().filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        profileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            }
        });

        // Set Click Listeners for Icons
        menuIcon.setOnClickListener(v -> {
            // Handle menu icon click (open/close drawer)
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Handle Search Input
        searchInput.setOnClickListener(v -> {
            // Handle search input click
            Toast.makeText(this, "Search Input Clicked", Toast.LENGTH_SHORT).show();
        });

        // Set up the Drawer Toggle
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Handle Navigation Item Clicks
        navigationView.setNavigationItemSelectedListener(this);

        // Check and request notification permission for Android 13 (Tiramisu) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Google Sign-In
        gOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gClient = GoogleSignIn.getClient(this, gOptions);

        GoogleSignInAccount gAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (gAccount != null) {
            String gName = gAccount.getDisplayName();
            // userName.setText(gName); // Assuming you have a TextView with id userName
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize RecyclerView
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesList = new ArrayList<>();
        noteAdapter = new NoteAdapter(this, notesList,false,false);
        notesRecyclerView.setAdapter(noteAdapter);

        // Initialize Empty State Text
        emptyStateText = findViewById(R.id.emptyStateText);

        // Fetch Notes from Firestore in real-time
        setupNotesListener();

        // Floating Action Button
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddNoteActivity.class);
            startActivityForResult(intent, 1);
        });

        // Initialize Location Client
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if this is the first launch after login
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        isFirstLaunch = prefs.getBoolean("first_launch", true);

        if (isFirstLaunch) {
            showLocationPermissionExplanation();
            prefs.edit().putBoolean("first_launch", false).apply();
        } else {
            checkLocationAndGPS();
        }

        // Create Notification Channel
        createNotificationChannel();

        if (ServiceUtils.isServiceRunning(this, LocationForegroundService.class)) {
            Toast.makeText(this, "Foreground Service is running", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Foreground Service is NOT running", Toast.LENGTH_SHORT).show();
        }

    }

    private void showLocationPermissionExplanation() {
        new AlertDialog.Builder(this)
                .setTitle("Location Access Needed")
                .setMessage("This app needs location access to provide location-based services. Please enable location permissions and GPS.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    checkLocationAndGPS();
                })
                .setNegativeButton("Later", (dialog, which) -> {
                    Toast.makeText(this, "You can enable location services later in settings", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    private void checkLocationAndGPS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            checkGPSEnabled();
        }
    }

    private void checkGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isGpsEnabled) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable GPS")
                    .setMessage("For accurate location services, please enable GPS on your device.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, GPS_REQUEST_CODE);
                    })
                    .setNegativeButton("Later", null)
                    .show();
        } else {
            startLocationServices();
        }
    }

    private void startLocationServices() {
        startLocationUpdates();
        startLocationForegroundService();
        fetchLastKnownLocation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_REQUEST_CODE) {
            // Check if GPS was enabled after returning from settings
            checkGPSEnabled();
        }
    }

    private void startLocationForegroundService() {
        Intent serviceIntent = new Intent(this, LocationForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopLocationForegroundService() {
        Intent serviceIntent = new Intent(this, LocationForegroundService.class);
        stopService(serviceIntent);
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.e("MainActivity", "Location result is null");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Log.d("MainActivity", "Location update: " + latitude + ", " + longitude);
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            Log.e("MainActivity", "Location permission not granted");
        }
    }

    private void fetchLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Log.d("MainActivity", "User location: " + latitude + ", " + longitude);
                        } else {
                            Log.e("MainActivity", "Location is null");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MainActivity", "Failed to get location: " + e.getMessage());
                    });
        } else {
            Log.e("MainActivity", "Location permission not granted");
        }
    }

    private void deleteNote(String noteId, Map<String, Object> note) {
        NoteManager.deleteNote(this, noteId, note, new NoteManager.OnNoteOperationListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Note moved to bin", Toast.LENGTH_SHORT).show();
                notesList.removeIf(n -> noteId.equals(n.get("id")));
                noteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void archiveNote(String noteId, Map<String, Object> note) {
        NoteManager.archiveNote(this, noteId, note, new NoteManager.OnNoteOperationListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Note archived", Toast.LENGTH_SHORT).show();
                notesList.removeIf(n -> noteId.equals(n.get("id")));
                noteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "NoteReminderChannel";
            String description = "Channel for Note Reminder Notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("note_reminder", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setupNotesListener() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            notesListener = db.collection("users").document(userId).collection("notes")
                    .addSnapshotListener((queryDocumentSnapshots, e) -> {
                        if (e != null) {
                            Log.e("MainActivity", "Failed to listen for notes: " + e.getMessage());
                            Toast.makeText(this, "Failed to listen for notes", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        notesList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            notesList.add(document.getData());
                            Log.d("MainActivity", "Fetched note: " + document.getData());
                            checkNoteForNotification(document.getData());
                        }
                        noteAdapter.notifyDataSetChanged();
                        noteAdapter.updateFullList(new ArrayList<>(notesList));

                        if (notesList.isEmpty()) {
                            emptyStateText.setVisibility(View.VISIBLE);
                        } else {
                            emptyStateText.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private void checkNoteForNotification(Map<String, Object> note) {
        if (note.containsKey("reminderTime")) {
            long reminderTime = (long) note.get("reminderTime");
            long currentTime = Calendar.getInstance().getTimeInMillis();

            if (reminderTime > currentTime) {
                Data inputData = new Data.Builder()
                        .putString("title", note.get("title").toString())
                        .putString("content", note.get("content").toString())
                        .build();

                OneTimeWorkRequest notificationWork = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                        .setInputData(inputData)
                        .setInitialDelay(reminderTime - currentTime, TimeUnit.MILLISECONDS)
                        .build();

                WorkManager.getInstance(this).enqueue(notificationWork);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notesListener != null) {
            notesListener.remove();
        }
        if (locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
        stopLocationForegroundService();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_notes) {
            notesRecyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(notesList.isEmpty() ? View.VISIBLE : View.GONE);
            findViewById(R.id.fragment_container).setVisibility(View.GONE);
            Toast.makeText(this, "Notes Selected", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_archive) {
            Intent archiveIntent = new Intent(this, ArchiveActivity.class);
            startActivity(archiveIntent);
        } else if (id == R.id.nav_bin) {
            Intent binIntent = new Intent(this, BinActivity.class);
            startActivity(binIntent);
        } else if (id == R.id.nav_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (findViewById(R.id.fragment_container).getVisibility() == View.VISIBLE) {
            notesRecyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(notesList.isEmpty() ? View.VISIBLE : View.GONE);
            findViewById(R.id.fragment_container).setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Location permission granted");
                checkGPSEnabled();
            } else {
                Log.e("MainActivity", "Location permission denied");
                Toast.makeText(this, "Location permission is required for geofencing", Toast.LENGTH_SHORT).show();
            }
        }
    }
}