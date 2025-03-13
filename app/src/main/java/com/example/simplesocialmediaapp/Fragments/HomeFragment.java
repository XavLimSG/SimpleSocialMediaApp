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
                int postsize = postids.size();
                postids.clear();

                if (postsize > 0)
                {
                    postSearchAdapter.notifyItemRangeRemoved(0,postsize);
                }

                if (!sv_searchpost.getQuery().equals(""))
                {
                    String text = sv_searchpost.getQuery().toString().toLowerCase();

                    CollectionReference collectionReference = firebaseFirestore.collection("Posts");
                    collectionReference.whereArrayContains("content",text).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            for (DocumentSnapshot snapshot1 : queryDocumentSnapshots)
                            {
                                PostSearchModel postSearchModel = snapshot1.toObject(PostSearchModel.class);
                                postids.add(postSearchModel.getPostid());
                                postSearchAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }

                return false;
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