package com.example.simplesocialmediaapp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplesocialmediaapp.R;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    private ArrayList<String> members = new ArrayList<>();
    private String circleId;
    private boolean isAdmin;

    public MembersAdapter(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public void setMembers(ArrayList<String> members, String circleId) {
        this.members = members;
        this.circleId = circleId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        String uid = members.get(position);
        holder.tvMemberUid.setText(uid);

        if (isAdmin) {
            holder.btnRemove.setVisibility(View.VISIBLE);
            holder.btnRemove.setOnClickListener(v -> removeUserFromCircle(uid));
        } else {
            holder.btnRemove.setVisibility(View.GONE);
        }
    }

    private void removeUserFromCircle(String uid) {
        // 1) Pull current members from "Circles/circleId"
        // 2) remove 'uid'
        FirebaseDatabase.getInstance().getReference("Circles")
                .child(circleId)
                .child("members")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        ArrayList<String> updated = (ArrayList<String>) task.getResult().getValue();
                        if (updated != null && updated.contains(uid)) {
                            updated.remove(uid);
                            FirebaseDatabase.getInstance().getReference("Circles")
                                    .child(circleId)
                                    .child("members")
                                    .setValue(updated);
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvMemberUid;
        Button btnRemove;
        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberUid = itemView.findViewById(R.id.tvMemberUid);
            btnRemove = itemView.findViewById(R.id.btnRemoveMember);
        }
    }
}
