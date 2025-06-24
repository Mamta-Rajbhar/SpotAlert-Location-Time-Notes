package com.example.location;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddNoteActivity extends AppCompatActivity {

    private EditText titleInput, contentInput, placeInput, latitudeInput, longitudeInput;
    private TextView dateTimeText, latLngDisplay;
    private ImageButton mapIcon;
    private Calendar selectedDate;
    private int selectedHour, selectedMinute;
    private LatLng selectedLocation;
    private String selectedPlaceName;
    private static final int PICK_IMAGE_REQUEST = 1;
    private OnNoteSavedListener onNoteSavedListener; // Callback listener
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private ImageView selectedImage;
    private TextView imageInfoText;
    private Uri imageUri; // To store the selected image URI
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        // Initialize views
        titleInput = findViewById(R.id.titleInput);
        contentInput = findViewById(R.id.contentInput);
        placeInput = findViewById(R.id.placeInput);
        dateTimeText = findViewById(R.id.dateTimeText);
        mapIcon = findViewById(R.id.mapIcon);
        latitudeInput = findViewById(R.id.latitudeInput);
        longitudeInput = findViewById(R.id.longitudeInput);
        latLngDisplay = findViewById(R.id.latLngDisplay);
        ImageButton optionsButton = findViewById(R.id.optionsButton);
        selectedImage = findViewById(R.id.selectedImage);
        imageInfoText = findViewById(R.id.imageInfoText);

        // Get the OnNoteSavedListener from the singleton
        onNoteSavedListener = NoteCallbackManager.getInstance().getListener();

        // Set click listener for the options button
        optionsButton.setOnClickListener(v -> showOptionsMenu(v));

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Set up the back button in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Handle back button click
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onSupportNavigateUp());

        // Set up "Save Place" button click listener
        Button savePlaceButton = findViewById(R.id.savePlaceButton);
        savePlaceButton.setOnClickListener(v -> onSavePlaceClick());
    }

    // Callback interface for note saving
    public interface OnNoteSavedListener extends java.io.Serializable {
        void onNoteSaved(String noteId, double latitude, double longitude);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            saveImageLocally(imageUri); // Save the image locally
        }

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String placeName = data.getStringExtra("placeName");
            double latitude = data.getDoubleExtra("latitude", 0);
            double longitude = data.getDoubleExtra("longitude", 0);

            // Update the place, latitude, and longitude fields
            placeInput.setText(placeName);
            latitudeInput.setText(String.valueOf(latitude));
            longitudeInput.setText(String.valueOf(longitude));
        }
    }

    private void saveImageLocally(Uri imageUri) {
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "External storage is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create a file in the app's internal storage
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imageFile = File.createTempFile("IMG_", ".jpg", storageDir);
            String imageFilePath = imageFile.getAbsolutePath();

            // Copy the selected image to the new file
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri);
                 OutputStream outputStream = new FileOutputStream(imageFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }

            // Display the image and its information
            selectedImage.setImageURI(imageUri);
            selectedImage.setVisibility(View.VISIBLE);
            //   imageInfoText.setText("Image saved at: " + imageFilePath);
            // imageInfoText.setVisibility(View.VISIBLE);

            // Set click listener to open the image
            selectedImage.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(imageUri, "image/*");
                startActivity(intent);
            });

            // Store the image URI for later use when saving the note
            this.imageUri = imageUri;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }


    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void updateNoteWithImagePath(String imageFilePath) {
        String userId = auth.getCurrentUser().getUid();
        String noteId = db.collection("users").document(userId).collection("notes").document().getId();

        Map<String, Object> note = new HashMap<>();
        note.put("imagePath", imageFilePath);

        db.collection("users").document(userId).collection("notes").document(noteId)
                .set(note)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Note updated with image path", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update note with image path", Toast.LENGTH_SHORT).show();
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    // Method to show the options menu
    public void showOptionsMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.note_options_menu);
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_add_image) {
                openImagePicker(); // Open image picker
                return true;
            } else {
                return false;
            }
        });
        popupMenu.show();
    }

    // Handle "Save Place" button click
    private void onSavePlaceClick() {
        String placeName = placeInput.getText().toString().trim();
        if (!placeName.isEmpty()) {
            new FetchCoordinatesTask().execute(placeName);
        } else {
            Toast.makeText(this, "Please enter a place name", Toast.LENGTH_SHORT).show();
        }
    }

    // AsyncTask to fetch latitude and longitude using Geocoder
    private class FetchCoordinatesTask extends AsyncTask<String, Void, LatLng> {
        @Override
        protected LatLng doInBackground(String... placeNames) {
            String placeName = placeNames[0];
            Geocoder geocoder = new Geocoder(AddNoteActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(placeName, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    double latitude = address.getLatitude();
                    double longitude = address.getLongitude();
                    return new LatLng(latitude, longitude);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("FetchCoordinatesTask", "Geocoder error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(LatLng latLng) {
            if (latLng != null) {
                latitudeInput.setText(String.valueOf(latLng.latitude));
                longitudeInput.setText(String.valueOf(latLng.longitude));
                selectedLocation = latLng;

                // Display latitude and longitude in the TextView
                String latLngText = "Latitude: " + latLng.latitude + ", Longitude: " + latLng.longitude;
                latLngDisplay.setText(latLngText);
            } else {
                Toast.makeText(AddNoteActivity.this, "Could not find coordinates for the place", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void showDateTimePicker(View view) {
        final Calendar currentDate = Calendar.getInstance();

        // Date Picker Dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
            selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);

            // Time Picker Dialog
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view2, hourOfDay, minute) -> {
                        selectedHour = hourOfDay;
                        selectedMinute = minute;
                        updateDateTimeText(); // Update the date and time text view
                    },
                    currentDate.get(Calendar.HOUR_OF_DAY),
                    currentDate.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(this) // Respect user's device time setting
            );

            timePickerDialog.show();
        },
                currentDate.get(Calendar.YEAR),
                currentDate.get(Calendar.MONTH),
                currentDate.get(Calendar.DAY_OF_MONTH));

        // Disable past dates in the date picker
        datePickerDialog.getDatePicker().setMinDate(currentDate.getTimeInMillis());
        datePickerDialog.show();
    }

    private void updateDateTimeText() {
        if (selectedDate != null) {
            // Format the date
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

            // Format the time based on the user's device settings (12-hour or 24-hour)
            String timeFormat = DateFormat.is24HourFormat(this) ? "HH:mm" : "hh:mm a";
            SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat, Locale.getDefault());

            // Set the selected time to the Calendar instance
            selectedDate.set(Calendar.HOUR_OF_DAY, selectedHour);
            selectedDate.set(Calendar.MINUTE, selectedMinute);

            // Combine date and time
            String formattedDateTime = dateFormat.format(selectedDate.getTime()) + ", " + timeFormatter.format(selectedDate.getTime());
            dateTimeText.setText("Selected Date and Time: " + formattedDateTime);
        } else {
            dateTimeText.setText("No date and time selected");
        }
    }

    public void openMap(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivityForResult(intent, 1); // Use startActivityForResult to get the result back
    }


    public void saveNote(View view) {
        String title = titleInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();
        String place = placeInput.getText().toString().trim();
        String latitudeStr = latitudeInput.getText().toString().trim();
        String longitudeStr = longitudeInput.getText().toString().trim();
        String dateTime = dateTimeText.getText().toString();

        // Check for location services
        checkLocationServices();

        // Check for location permission
        checkLocationPermission();

        // Check for Google Play Services
        checkGooglePlayServices();

        // Mandatory fields validation
        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Title and Note cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse latitude and longitude if provided
        final Double latitude; // Declare as final
        final Double longitude; // Declare as final
        if (!latitudeStr.isEmpty() && !longitudeStr.isEmpty()) {
            try {
                latitude = Double.parseDouble(latitudeStr);
                longitude = Double.parseDouble(longitudeStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid latitude or longitude values", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            latitude = null;
            longitude = null;
        }

        String userId = auth.getCurrentUser().getUid();
        final String noteId = db.collection("users").document(userId).collection("notes").document().getId();

        // Create the note object
        Map<String, Object> note = new HashMap<>();
        note.put("id", noteId);
        note.put("title", title);
        note.put("content", content);
        note.put("dateTime", dateTime);

        // Add place details if provided
        if (!place.isEmpty() && latitude != null && longitude != null) {
            Map<String, Object> placeDetails = new HashMap<>();
            placeDetails.put("name", place);
            placeDetails.put("latitude", latitude);
            placeDetails.put("longitude", longitude);
            note.put("place", placeDetails);
        }

        // Add reminder time if date and time are selected
        if (selectedDate != null) {
            long reminderTime = selectedDate.getTimeInMillis();
            note.put("reminderTime", reminderTime);
        }

        // Add image path if an image was selected
        if (imageUri != null) {
            note.put("imagePath", imageUri.toString());
        }

        // Save the note to Firestore
        db.collection("users").document(userId).collection("notes").document(noteId)
                .set(note)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Note Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("AddNoteActivity", "Error saving note: " + e.getMessage());
                    Toast.makeText(this, "Failed to save note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkLocationServices() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Location services are disabled, prompt the user to enable them
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Required")
                        .setMessage("This app needs location permission to save location-based notes.")
                        .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(
                                this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                LOCATION_PERMISSION_REQUEST_CODE
                        ))
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE
                );
            }
        } else {
            // Permission is already granted
            Log.d("AddNoteActivity", "Location permission already granted");
        }
    }

    private void checkGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            // Google Play Services is not available or outdated
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                // Prompt the user to update Google Play Services
                googleApiAvailability.getErrorDialog(this, resultCode, 9000).show();
            } else {
                Toast.makeText(this, "Google Play Services is not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Close the activity when the back button is pressed
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("AddNoteActivity", "Location permission granted");
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}