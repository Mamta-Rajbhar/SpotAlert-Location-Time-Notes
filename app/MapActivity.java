package com.example.location;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap googleMap;
    private SearchView searchView;
    private Marker selectedMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Initialize Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize SearchView
        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchPlace(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Set the initial map view to India
        LatLng india = new LatLng(20.5937, 78.9629); // Coordinates for India
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 4)); // Zoom level for India

        // Set a marker click listener
        googleMap.setOnMarkerClickListener(this);

        // Set a map click listener to add a marker at the clicked location
        googleMap.setOnMapClickListener(latLng -> {
            if (selectedMarker != null) {
                selectedMarker.remove(); // Remove the previous marker
            }
            selectedMarker = googleMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15)); // Zoom to the selected location
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // Get the place details from the marker
        LatLng latLng = marker.getPosition();
        String placeName = marker.getTitle();

        // Return the place details to AddNoteActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("placeName", placeName);
        resultIntent.putExtra("latitude", latLng.latitude);
        resultIntent.putExtra("longitude", latLng.longitude);
        setResult(RESULT_OK, resultIntent);
        finish();
        return true;
    }

    private void searchPlace(String query) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                // Clear previous markers
                googleMap.clear();

                // Add a marker for the searched place
                selectedMarker = googleMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(address.getFeatureName()));

                // Move the camera to the searched place
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("MapActivity", "Geocoder error: " + e.getMessage());
        }
    }
}