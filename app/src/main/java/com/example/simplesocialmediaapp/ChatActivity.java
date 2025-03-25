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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat); // Make sure this layout exists

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

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
}
