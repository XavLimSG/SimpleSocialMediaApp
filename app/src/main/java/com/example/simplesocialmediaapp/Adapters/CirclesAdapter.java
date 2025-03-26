package com.example.simplesocialmediaapp.Adapters;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplesocialmediaapp.CircleDetailActivity;
import com.example.simplesocialmediaapp.Models.CircleModel;
import com.example.simplesocialmediaapp.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class CirclesAdapter extends RecyclerView.Adapter<CirclesAdapter.CirclesViewHolder> {

    private ArrayList<CircleModel> circles;
    private boolean isAdmin;
    private String currentUserUid;

    public CirclesAdapter(ArrayList<CircleModel> circles, boolean isAdmin, String currentUserUid) {
        this.circles = circles;
        this.isAdmin = isAdmin;
        this.currentUserUid = currentUserUid;
    }

    @NonNull
    @Override
    public CirclesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_circle, parent, false);
        return new CirclesViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CirclesViewHolder holder, int position) {
        CircleModel circle = circles.get(position);
        holder.tvCircleName.setText(circle.getCircleName());

        int count = (circle.getMembers() == null) ? 0 : circle.getMembers().size();
        holder.tvMembers.setText("Members: " + count);

        // Clicking the entire item -> open detail activity
        holder.itemView.setOnClickListener(v -> {
            CircleDetailActivity.launch(
                    v.getContext(),
                    circle.getCircleId(),
                    circle.getCircleName(),
                    isAdmin
            );
        });

        if (isAdmin) {
            holder.btnAddMember.setVisibility(View.VISIBLE);
            holder.btnDeleteCircle.setVisibility(View.VISIBLE);

            holder.btnAddMember.setOnClickListener(v ->
                    showAddMemberDialog(holder.itemView.getContext(), circle)
            );

            holder.btnDeleteCircle.setOnClickListener(v -> {
                DatabaseReference circleRef = FirebaseDatabase.getInstance()
                        .getReference("Circles")
                        .child(circle.getCircleId());
                circleRef.removeValue();
            });
        } else {
            holder.btnAddMember.setVisibility(View.GONE);
            holder.btnDeleteCircle.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return circles.size();
    }

    private void showAddMemberDialog(android.content.Context context, CircleModel circle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add Member to " + circle.getCircleName());
        final EditText input = new EditText(context);
        input.setHint("Enter new member EMAIL");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String userEmail = input.getText().toString().trim().toLowerCase();
            if (!userEmail.isEmpty()) {
                String emailKey = userEmail.replace(".", "_dot_");
                DatabaseReference emailToUidRef = FirebaseDatabase.getInstance().getReference("EmailToUid");
                emailToUidRef.child(emailKey).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String newMemberUid = task.getResult().getValue(String.class);
                        addUserToCircle(context, newMemberUid, circle);
                    } else {
                        Toast.makeText(context, "No user found with that email", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addUserToCircle(android.content.Context context, String newMemberUid, CircleModel circle) {
        if (circle.getMembers() == null) {
            circle.setMembers(new ArrayList<>());
        }
        if (!circle.getMembers().contains(newMemberUid)) {
            circle.getMembers().add(newMemberUid);

            DatabaseReference circleRef = FirebaseDatabase.getInstance()
                    .getReference("Circles")
                    .child(circle.getCircleId());
            circleRef.setValue(circle).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(context, "Member added!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static class CirclesViewHolder extends RecyclerView.ViewHolder {
        TextView tvCircleName, tvMembers;
        Button btnAddMember, btnDeleteCircle;

        public CirclesViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCircleName = itemView.findViewById(R.id.tvCircleName);
            tvMembers = itemView.findViewById(R.id.tvMembers);
            btnAddMember = itemView.findViewById(R.id.btnAddMember);
            btnDeleteCircle = itemView.findViewById(R.id.btnDeleteCircle);
        }
    }
}
