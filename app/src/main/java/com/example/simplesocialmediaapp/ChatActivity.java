package com.example.simplesocialmediaapp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplesocialmediaapp.Adapters.ChatAdapter;
import com.example.simplesocialmediaapp.Models.MessageModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// loation stuff
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.widget.ImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ChatAdapter chatAdapter;
    private List<MessageModel> messageList;

    private DatabaseReference chatRef;
    private String conversationId;
    private String currentUserId;
    private String otherUserId;

    private ImageButton buttonShareLocation; // location share button

    private static final int PERMISSION_REQUEST_CODE = 100; //Yummy

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request all necessary permissions
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat); // Make sure this layout exists

        checkAndRequestPermissions(); //Yummy

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        // location stuff
        buttonShareLocation = findViewById(R.id.buttonShareLocation);
        buttonShareLocation.setOnClickListener(v -> {
            shareLocation();
        });



        // Get current user and recipient IDs
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        otherUserId = getIntent().getStringExtra("uid");
        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Error: Chat partner not specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        // Create a conversation ID by sorting the two IDs (order independent)
        if (currentUserId.compareTo(otherUserId) < 0) {
            conversationId = currentUserId + "_" + otherUserId;
        } else {
            conversationId = otherUserId + "_" + currentUserId;
        }

        // Store references for both participants
        DatabaseReference userChatsRef = FirebaseDatabase.getInstance().getReference("UserChats");
        // For current user
        userChatsRef.child(currentUserId).child(conversationId).setValue(true);
        // For other user
        userChatsRef.child(otherUserId).child(conversationId).setValue(true);

        // Reference the chat node in Firebase Realtime Database
        chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(conversationId);

        // Set up the RecyclerView with the chat adapter
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // Listen for new messages in this conversation
        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                MessageModel message = snapshot.getValue(MessageModel.class);
                if (message != null) {
                    messageList.add(message);
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) { }
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) { }
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) { }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Send a message when the send button is tapped
        buttonSend.setOnClickListener(v -> {
            String msg = editTextMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                MessageModel message = new MessageModel(currentUserId, otherUserId, msg, System.currentTimeMillis());
                chatRef.push().setValue(message);
                editTextMessage.setText("");
            }
        });
    }

    // location stuff
    private void shareLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            MessageModel locationMessage = new MessageModel(
                    currentUserId,
                    otherUserId,
                    " Shared a location",
                    System.currentTimeMillis()
            );

            locationMessage.setLatitude(lat);
            locationMessage.setLongitude(lng);
            locationMessage.setLocation(true); // mark as location message

            chatRef.push().setValue(locationMessage);
            Toast.makeText(this, "Location sent!", Toast.LENGTH_SHORT).show(); // Optional
        } else {
            Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
        }
    }



//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            shareLocation(); // Try again now that permission is granted
//        } else {
//            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
//        }
//    }

    //Yummy
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Permissions are required for the app to function properly.", Toast.LENGTH_LONG).show();
            }
        }
    }


}
