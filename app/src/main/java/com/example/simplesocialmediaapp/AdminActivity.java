package com.example.simplesocialmediaapp;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Simple display to confirm we are in the admin area
        TextView tvAdmin = findViewById(R.id.tvAdmin);
        tvAdmin.setText("Welcome to the Admin Dashboard");
    }
}
