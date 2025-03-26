package com.example.simplesocialmediaapp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.example.simplesocialmediaapp.Models.LocationModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference locationRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mAuth = FirebaseAuth.getInstance();
        String uid = mAuth.getCurrentUser().getUid();

        // Point to your Firebase location data
        locationRef = FirebaseDatabase.getInstance()
                .getReference("Locations")
                .child(uid);

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Get your location from Firebase and show it
        locationRef.get().addOnSuccessListener(snapshot -> {
            LocationModel location = snapshot.getValue(LocationModel.class);
            if (location != null) {
                LatLng myLoc = new LatLng(location.latitude, location.longitude);
                mMap.addMarker(new MarkerOptions().position(myLoc).title("My Last Shared Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLoc, 16));
            }
        }).addOnFailureListener(e -> {
            e.printStackTrace();
        });
    }
}
