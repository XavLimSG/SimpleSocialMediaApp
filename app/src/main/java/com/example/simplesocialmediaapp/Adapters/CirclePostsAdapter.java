package com.example.simplesocialmediaapp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.simplesocialmediaapp.Models.PostModel;
import com.example.simplesocialmediaapp.R;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class CirclePostsAdapter extends RecyclerView.Adapter<CirclePostsAdapter.PostViewHolder> {

    private ArrayList<PostModel> posts = new ArrayList<>();
    private boolean isAdmin;
    private String circleId;

    // We'll do a quick Firestore lookup to get display name from "Profiles"
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CirclePostsAdapter(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public void setPosts(ArrayList<PostModel> posts, String circleId) {
        this.posts = posts;
        this.circleId = circleId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CirclePostsAdapter.PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_circle_post, parent, false);
        return new PostViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CirclePostsAdapter.PostViewHolder holder, int position) {
        PostModel post = posts.get(position);

        // Load user's display name or email from Firestore "Profiles" by doc ID = post.getUid()
        db.collection("Profiles").document(post.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String displayName = documentSnapshot.getString("username");
                if (displayName == null || displayName.isEmpty()) {
                    // fallback to email if no username
                    displayName = documentSnapshot.getString("email");
                }
                holder.tvPostOwner.setText(displayName != null ? displayName : post.getUid());
            } else {
                // fallback if not found
                holder.tvPostOwner.setText(post.getUid());
            }
        });

        holder.tvPostContent.setText(post.getContent());

        if (isAdmin) {
            holder.btnDeletePost.setVisibility(View.VISIBLE);
            holder.btnDeletePost.setOnClickListener(v -> {
                // remove from "CirclePosts/circleId/<timestamp>"
                String pushId = post.getTimestamp();
                FirebaseDatabase.getInstance().getReference("CirclePosts")
                        .child(circleId)
                        .child(pushId)
                        .removeValue();
            });
        } else {
            holder.btnDeletePost.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvPostContent, tvPostOwner;
        Button btnDeletePost;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvPostOwner = itemView.findViewById(R.id.tvPostOwner);
            btnDeletePost = itemView.findViewById(R.id.btnDeleteCirclePost);
        }
    }
}
