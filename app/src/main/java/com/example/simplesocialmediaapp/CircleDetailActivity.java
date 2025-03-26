package com.example.simplesocialmediaapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplesocialmediaapp.Adapters.CirclePostsAdapter;
import com.example.simplesocialmediaapp.Adapters.MembersAdapter;
import com.example.simplesocialmediaapp.Models.CircleModel;
import com.example.simplesocialmediaapp.Models.PostModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/**
 * Displays details for a circle, including:
 *   - List of members (with remove button if admin)
 *   - List of circle posts (with delete button if admin)
 *   - Button for creating new post in this circle (if admin)
 */
public class CircleDetailActivity extends AppCompatActivity {

    private static final String EXTRA_CIRCLE_ID = "circle_id";
    private static final String EXTRA_CIRCLE_NAME = "circle_name";
    private static final String EXTRA_IS_ADMIN = "is_admin";

    private String circleId;
    private String circleName;
    private boolean isAdmin;

    private TextView tvCircleName;
    private RecyclerView rvMembers;
    private RecyclerView rvPosts;
    private FloatingActionButton fabCreatePost;

    private MembersAdapter membersAdapter;
    private CirclePostsAdapter postsAdapter;

    private DatabaseReference circleRef;
    private DatabaseReference circlePostsRef;
    private CircleModel circleModel;

    /**
     * Launches the CircleDetailActivity with the specified circle ID, name, and admin flag.
     */
    public static void launch(Context context, String circleId, String circleName, boolean isAdmin) {
        Intent i = new Intent(context, CircleDetailActivity.class);
        i.putExtra(EXTRA_CIRCLE_ID, circleId);
        i.putExtra(EXTRA_CIRCLE_NAME, circleName);
        i.putExtra(EXTRA_IS_ADMIN, isAdmin);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circle_detail);

        // Enable back arrow:
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Circle Details");
        }

        // Retrieve extras
        circleId = getIntent().getStringExtra(EXTRA_CIRCLE_ID);
        circleName = getIntent().getStringExtra(EXTRA_CIRCLE_NAME);
        isAdmin = getIntent().getBooleanExtra(EXTRA_IS_ADMIN, false);

        // DB references
        circleRef = FirebaseDatabase.getInstance().getReference("Circles").child(circleId);
        circlePostsRef = FirebaseDatabase.getInstance().getReference("CirclePosts").child(circleId);

        // Init views
        tvCircleName = findViewById(R.id.tvCircleDetailName);
        rvMembers = findViewById(R.id.rvCircleMembers);
        rvPosts = findViewById(R.id.rvCirclePosts);
        fabCreatePost = findViewById(R.id.fabCreateCirclePost);

        tvCircleName.setText(circleName);

        // Setup members list
        membersAdapter = new MembersAdapter(isAdmin);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(membersAdapter);

        // Setup circle posts
        postsAdapter = new CirclePostsAdapter(isAdmin);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postsAdapter);

        if (isAdmin) {
            fabCreatePost.setVisibility(View.VISIBLE);
            fabCreatePost.setOnClickListener(v -> showCreatePostDialog());
        } else {
            fabCreatePost.setVisibility(View.GONE);
        }

        loadCircleData();
        loadCirclePosts();
    }

    // Listen for up arrow press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadCircleData() {
        circleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                circleModel = snapshot.getValue(CircleModel.class);
                if (circleModel != null && circleModel.getMembers() != null) {
                    membersAdapter.setMembers(circleModel.getMembers(), circleModel.getCircleId());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CircleDetailActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCirclePosts() {
        circlePostsRef.addValueEventListener(new ValueEventListener() {
            final ArrayList<PostModel> postList = new ArrayList<>();
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    PostModel post = ds.getValue(PostModel.class);
                    if (post != null) {
                        postList.add(post);
                    }
                }
                // Provide the entire updated list to the adapter
                postsAdapter.setPosts(postList, circleId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CircleDetailActivity.this, "Load posts error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreatePostDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Post in " + circleName);

        final EditText input = new EditText(this);
        input.setHint("Enter post content");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String content = input.getText().toString().trim();
            if (!content.isEmpty()) {
                createPost(content);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createPost(String content) {
        String postId = circlePostsRef.push().getKey();
        if (postId == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();

        PostModel post = new PostModel(
                uid,
                content,
                /* path= */ "",
                String.valueOf(timestamp),
                new ArrayList<>(),
                new ArrayList<>()
        );
        // We'll store it under "circlePostsRef/<timestamp>"
        circlePostsRef.child(String.valueOf(timestamp)).setValue(post).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Post created", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to create post", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
