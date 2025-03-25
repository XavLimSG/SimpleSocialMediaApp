package com.example.simplesocialmediaapp.Adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.simplesocialmediaapp.ChatActivity;
import com.example.simplesocialmediaapp.R;

import java.util.List;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.simplesocialmediaapp.Models.ProfileModel;


public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ConvViewHolder> {

    private List<String> conversationIds;
    private String currentUserId;

    public ConversationsAdapter(List<String> conversationIds, String currentUserId) {
        this.conversationIds = conversationIds;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ConvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_item, parent, false);
        return new ConvViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull ConvViewHolder holder, int position) {
        String conversationId = conversationIds.get(position);
        // Parse out the other user's ID
        String[] parts = conversationId.split("_");
        if (parts.length == 2) {
            String userA = parts[0];
            String userB = parts[1];
            String otherUserId = currentUserId.equals(userA) ? userB : userA;

            FirebaseFirestore.getInstance().collection("Profiles")
                    .document(otherUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            ProfileModel profile = documentSnapshot.toObject(ProfileModel.class);
                            if (profile != null) {
                                // Show the username instead of the raw user ID
                                holder.tvConversation.setText("Chat with: " + profile.getUsername());
                            } else {
                                // If profile is null, fallback to the ID
                                holder.tvConversation.setText("Chat with: " + otherUserId);
                            }
                        } else {
                            // If doc doesn't exist, fallback to the ID
                            holder.tvConversation.setText("Chat with: " + otherUserId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // If there's an error fetching, fallback to the ID
                        holder.tvConversation.setText("Chat with: " + otherUserId);
                    });

            // Launch ChatActivity on click
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), ChatActivity.class);
                intent.putExtra("uid", otherUserId);  // pass the other user's ID
                holder.itemView.getContext().startActivity(intent);
            });
        } else {
            // If the ID doesn't split properly, just display it
            holder.tvConversation.setText("Conversation: " + conversationId);
        }
    }

    @Override
    public int getItemCount() {
        return conversationIds.size();
    }

    public static class ConvViewHolder extends RecyclerView.ViewHolder {
        TextView tvConversation;

        public ConvViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConversation = itemView.findViewById(R.id.tv_conversation);
        }
    }
}
