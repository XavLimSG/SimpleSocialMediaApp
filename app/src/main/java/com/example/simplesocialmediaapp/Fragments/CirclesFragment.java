package com.example.simplesocialmediaapp.Fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplesocialmediaapp.Adapters.CirclesAdapter;
import com.example.simplesocialmediaapp.Models.CircleModel;
import com.example.simplesocialmediaapp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class CirclesFragment extends Fragment {

    private RecyclerView rvCircles;
    private FloatingActionButton fabAddCircle;

    private boolean isAdmin = false;
    private String currentUserUid;

    private DatabaseReference circlesRef;
    private ArrayList<CircleModel> circlesList;
    private CirclesAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Grab isAdmin from the parent activity
        if (getActivity() != null && getActivity().getIntent() != null) {
            isAdmin = getActivity().getIntent().getBooleanExtra("isAdmin", false);
        }
        currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Realtime Database reference
        circlesRef = FirebaseDatabase.getInstance().getReference("Circles");

        circlesList = new ArrayList<>();
        adapter = new CirclesAdapter(circlesList, isAdmin, currentUserUid);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View item = inflater.inflate(R.layout.fragment_circles, container, false);

        rvCircles = item.findViewById(R.id.rv_circles);
        rvCircles.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCircles.setAdapter(adapter);

        fabAddCircle = item.findViewById(R.id.fab_add_circle);
        fabAddCircle.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

        fabAddCircle.setOnClickListener(v -> showCreateCircleDialog());

        loadCircles();

        return item;
    }

    private void loadCircles() {
        // Show all circles if admin, else only circles that contain the user's UID
        circlesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                circlesList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    CircleModel circle = ds.getValue(CircleModel.class);
                    if (circle != null) {
                        // Admin sees all, students see only membership circles
                        if (isAdmin || (circle.getMembers() != null && circle.getMembers().contains(currentUserUid))) {
                            circlesList.add(circle);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load circles: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateCircleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Create Circle");

        final EditText input = new EditText(getContext());
        input.setHint("Enter circle name");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String circleName = input.getText().toString().trim();
            if (!circleName.isEmpty()) {
                createCircle(circleName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createCircle(String circleName) {
        String circleId = circlesRef.push().getKey();
        if (circleId == null) return;

        // By default, add teacher to the circle
        ArrayList<String> members = new ArrayList<>();
        members.add(currentUserUid);

        CircleModel newCircle = new CircleModel(circleId, circleName, members);
        circlesRef.child(circleId).setValue(newCircle).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Circle created", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to create circle", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
