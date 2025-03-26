package com.example.simplesocialmediaapp.Adapters;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.simplesocialmediaapp.Models.MessageModel;
import com.example.simplesocialmediaapp.R;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<MessageModel> messages;

    public ChatAdapter(List<MessageModel> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate a layout for each chat message (chat_item.xml)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        MessageModel message = messages.get(position);
        holder.textMessage.setText(message.getMessage());

        // Check if the message is sent or received and set gravity and background accordingly
        if (message.getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            // Sent message (light blue background, right-aligned)
            holder.textMessage.setBackgroundColor(Color.parseColor("#D0E7FF"));
            ((LinearLayout.LayoutParams) holder.textMessage.getLayoutParams()).gravity = Gravity.END;
            ((LinearLayout.LayoutParams) holder.textTimestamp.getLayoutParams()).gravity = Gravity.END;
        } else {
            // Received message (white background, left-aligned)
            holder.textMessage.setBackgroundColor(Color.parseColor("#EDEDEB"));
            ((LinearLayout.LayoutParams) holder.textMessage.getLayoutParams()).gravity = Gravity.START;
            ((LinearLayout.LayoutParams) holder.textTimestamp.getLayoutParams()).gravity = Gravity.START;
        }

        // Convert timestamp to a readable date/time
        String dateString = DateFormat.getDateTimeInstance().format(message.getTimestamp());
        holder.textTimestamp.setText(dateString);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textTimestamp;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
        }
    }
}
