package com.example.simplesocialmediaapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.simplesocialmediaapp.Fragments.ChatsFragment;
import com.example.simplesocialmediaapp.Fragments.CirclesFragment;
import com.example.simplesocialmediaapp.Fragments.CreateFragment;
import com.example.simplesocialmediaapp.Fragments.HomeFragment;
import com.example.simplesocialmediaapp.Fragments.ProfileFragment;
import com.example.simplesocialmediaapp.Models.MessageModel;
import com.example.simplesocialmediaapp.Models.ProfileModel;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;

/**
 * Shows how to open ChatActivity for the *specific user* that sent the message
 * when tapping the notification, by putting "uid" in the Intent to ChatActivity.
 */
public class HomeActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    TabLayout apptabs;
    ViewPager2 pager;
    ViewPagerFragmentAdapter adapter;
    boolean isAdmin = false;

    private static final String CHANNEL_ID = "IN_APP_CHAT_NOTIF_CHANNEL";
    private static final int NOTIF_ID = 999;

    private DatabaseReference userChatsRef;
    private String currentUserId;

    // We'll store push keys we've already shown a notification for
    private Set<String> notifiedKeys = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // For edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1) Create notification channel for in-app notifications
        createNotificationChannel();

        // 2) Read admin flag passed from SignInActivity
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);

        initvar();

        // If the user is not admin, remove the 3rd tab
        if (!isAdmin && apptabs.getTabCount() > 2) {
            apptabs.removeTabAt(2);
        }

        // Rebuild the ViewPager
        adapter = new ViewPagerFragmentAdapter(this, apptabs.getTabCount(), isAdmin);
        pager.setAdapter(adapter);

        settabs();

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                apptabs.selectTab(apptabs.getTabAt(position));
            }
        });

        // 3) Now attach the global new-message listener
        setupGlobalChatListener();
    }

    void initvar() {
        mAuth = FirebaseAuth.getInstance();
        apptabs = findViewById(R.id.apptabs);
        pager = findViewById(R.id.pager);

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not signed in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        userChatsRef = FirebaseDatabase.getInstance()
                .getReference("UserChats")
                .child(currentUserId);
    }

    void settabs() {
        apptabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getIcon() != null) {
                    tab.getIcon().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                }
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getIcon() != null) {
                    tab.getIcon().setColorFilter(Color.parseColor("#4F78D0"), PorterDuff.Mode.SRC_IN);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // optional
            }
        });

        if (apptabs.getTabAt(0) != null) {
            if (apptabs.getTabAt(0).getIcon() != null) {
                apptabs.getTabAt(0).getIcon().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
            }
            apptabs.selectTab(apptabs.getTabAt(0));
            pager.setCurrentItem(0);
        }
    }

    // 4) Attach DB listeners for new messages
    private void setupGlobalChatListener() {
        userChatsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                String conversationId = snapshot.getKey();
                if (conversationId == null) return;

                DatabaseReference convRef = FirebaseDatabase.getInstance()
                        .getReference("Chats")
                        .child(conversationId);

                convRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot msgSnap, String p) {
                        MessageModel message = msgSnap.getValue(MessageModel.class);
                        if (message == null) return;

                        String msgKey = msgSnap.getKey();
                        if (msgKey == null) return;

                        // Only notify if from other user & haven't notified yet
                        if (!message.getSenderId().equals(currentUserId)
                                && !notifiedKeys.contains(msgKey)) {

                            notifiedKeys.add(msgKey);
                            fetchSenderNameAndNotify(message.getSenderId(), message.getMessage());
                        }
                    }
                    @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String p) {}
                    @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                    @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String p) {}
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(HomeActivity.this,
                                "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String prev) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String prev) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // 5) We'll fetch the sender name from Firestore, then pass the "senderId" into the intent
    private void fetchSenderNameAndNotify(String senderId, String msgText) {
        FirebaseFirestore.getInstance()
                .collection("Profiles")
                .document(senderId)
                .get()
                .addOnSuccessListener(doc -> {
                    String displayName = senderId;
                    if (doc.exists()) {
                        ProfileModel profile = doc.toObject(ProfileModel.class);
                        if (profile != null && profile.getUsername() != null) {
                            displayName = profile.getUsername();
                        }
                    }

                    String title = "New message from " + displayName;
                    showLocalNotification(title, msgText, senderId);
                })
                .addOnFailureListener(e -> {
                    // fallback
                    String title = "New message from " + senderId;
                    showLocalNotification(title, msgText, senderId);
                });
    }

    // 6) showLocalNotification now receives "senderId" so we can open ChatActivity for that user
    private void showLocalNotification(String title, String msg, String senderId) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // We'll open ChatActivity for the user who sent the message
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("uid", senderId);  // ChatActivity uses this to open conversation w/ that user

        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pi);

        manager.notify(NOTIF_ID, builder.build());
    }

    // 7) Create the local notification channel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "In-App Chat Notifications";
            String desc = "Notifications for new chat messages (while in background)";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(desc);

            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    // The usual adapter for your tabs
    public static class ViewPagerFragmentAdapter extends FragmentStateAdapter {
        int tabCount;
        boolean isAdmin;

        public ViewPagerFragmentAdapter(@NonNull FragmentActivity fragmentActivity, int tabCount, boolean isAdmin) {
            super(fragmentActivity);
            this.tabCount = tabCount;
            this.isAdmin = isAdmin;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (isAdmin) {
                // Admin: 0=Home,1=Circles,2=Create,3=Chats,4=Profile
                switch (position) {
                    case 0: return new HomeFragment();
                    case 1: return new CirclesFragment();
                    case 2: return new CreateFragment();
                    case 3: return new ChatsFragment();
                    case 4: return new ProfileFragment();
                    default: return new HomeFragment();
                }
            } else {
                // Student: 0=Home,1=Circles,2=Chats,3=Profile
                switch (position) {
                    case 0: return new HomeFragment();
                    case 1: return new CirclesFragment();
                    case 2: return new ChatsFragment();
                    case 3: return new ProfileFragment();
                    default: return new HomeFragment();
                }
            }
        }

        @Override
        public int getItemCount() {
            return tabCount;
        }
    }
}
