package com.example.simplesocialmediaapp.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.simplesocialmediaapp.Adapters.PostSearchAdapter;
import com.example.simplesocialmediaapp.Models.PostSearchModel;
import com.example.simplesocialmediaapp.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;



import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import android.content.Intent;
import android.widget.Toast;




/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    TabLayout tl_home;
    SearchView sv_searchpost;
    RecyclerView rv_top,rv_new,rv_search;
    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference("Posts");
    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tl_home.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0)
                {
                    rv_top.setVisibility(View.VISIBLE);
                    rv_new.setVisibility(View.GONE);
                    rv_search.setVisibility(View.GONE);

                    sv_searchpost.setVisibility(View.GONE);
                }
                else if (tab.getPosition() == 1)
                {
                    rv_top.setVisibility(View.GONE);
                    rv_new.setVisibility(View.VISIBLE);
                    rv_search.setVisibility(View.GONE);

                    sv_searchpost.setVisibility(View.GONE);
                }
                else if (tab.getPosition() == 2)
                {
                    rv_top.setVisibility(View.GONE);
                    rv_new.setVisibility(View.GONE);
                    rv_search.setVisibility(View.VISIBLE);

                    sv_searchpost.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        ArrayList<String> postids = new ArrayList<>();
        PostSearchAdapter postSearchAdapter = new PostSearchAdapter(postids);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false);
        rv_search.setLayoutManager(layoutManager);
        rv_search.setAdapter(postSearchAdapter);

        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String key = snapshot.getKey();
                int pos = postids.indexOf(key);
                if (pos >= 0)
                {
                    postSearchAdapter.notifyItemChanged(pos);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        sv_searchpost.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.isEmpty()) {
                    final String searchTerm = query.toLowerCase(); // assuming profiles are stored in lowercase

                    // Create two prefix queries for username and email
                    Query usernameQuery = firebaseFirestore.collection("Profiles")
                            .orderBy("username")
                            .startAt(searchTerm)
                            .endAt(searchTerm + "\uf8ff");

                    Query emailQuery = firebaseFirestore.collection("Profiles")
                            .orderBy("email")
                            .startAt(searchTerm)
                            .endAt(searchTerm + "\uf8ff");

                    // Execute the queries and merge results
                    usernameQuery.get().addOnSuccessListener(usernameSnapshot -> {
                        emailQuery.get().addOnSuccessListener(emailSnapshot -> {
                            // Merge both query results to avoid duplicates
                            HashSet<DocumentSnapshot> resultsSet = new HashSet<>();
                            resultsSet.addAll(usernameSnapshot.getDocuments());
                            resultsSet.addAll(emailSnapshot.getDocuments());

                            if (!resultsSet.isEmpty()) {
                                // Create a list of matching user IDs
                                List<String> matchingUserIds = new ArrayList<>();
                                for (DocumentSnapshot doc : resultsSet) {
                                    String userId = doc.getString("id");
                                    if (userId != null && !userId.isEmpty()) {
                                        matchingUserIds.add(userId);
                                    }
                                }

                                // Clear any previous post results from the adapter's list
                                postids.clear();
                                postSearchAdapter.notifyDataSetChanged();

                                // For each matching user, query posts in the Realtime Database
                                for (String userId : matchingUserIds) {
                                    FirebaseDatabase.getInstance().getReference("Posts")
                                            .orderByChild("uid")
                                            .equalTo(userId)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    for (DataSnapshot postSnap : snapshot.getChildren()) {
                                                        String postId = postSnap.getKey();
                                                        if (postId != null && !postids.contains(postId)) {
                                                            postids.add(postId);
                                                        }
                                                    }
                                                    postSearchAdapter.notifyDataSetChanged();
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    Toast.makeText(requireContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            } else {
                                Toast.makeText(requireContext(), "No matching profiles found", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(e ->
                                Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }).addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View item = inflater.inflate(R.layout.fragment_home, container, false);

        tl_home = item.findViewById(R.id.tl_home);

        rv_top = item.findViewById(R.id.rv_top);
        rv_new = item.findViewById(R.id.rv_new);
        rv_search = item.findViewById(R.id.rv_search);

        sv_searchpost = item.findViewById(R.id.sv_searchpost);

        return item;
    }
}