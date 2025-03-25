package com.example.simplesocialmediaapp.Fragments;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.simplesocialmediaapp.Adapters.PostContentAdapter;
import com.example.simplesocialmediaapp.Models.PostModel;
import com.example.simplesocialmediaapp.Models.PostReferenceModel;
import com.example.simplesocialmediaapp.Models.ProfileModel;
import com.example.simplesocialmediaapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import com.example.simplesocialmediaapp.SignInActivity;

import java.util.ArrayList;

public class ProfileFragment extends Fragment {

    CardView cv_profile_image;
    ImageView imv_profile;

    FirebaseAuth mAuth;
    FirebaseFirestore firebaseFirestore;
    CollectionReference collectionReference;

    private Button btnLogout;

    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    ProfileModel model;

    TextView tv_profile_posts,tv_profile_uname,tv_profile_email;

    RecyclerView rv_profile_content;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    ArrayList<PostReferenceModel> postReferenceModels = new ArrayList<>();
    ArrayList<String> keys = new ArrayList<>();
    ConstraintLayout cl_profile;

    public ProfileFragment() {
        // Required empty public constructor
    }


    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
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

        btnLogout = view.findViewById(R.id.btn_logout); // Find Log Out button

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser(); // Call logout function
            }
        });

        rv_profile_content.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cl_profile.requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        collectionReference.document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful())
                {
                    model = task.getResult().toObject(ProfileModel.class);
                    tv_profile_uname.setText(model.getUsername());
                    tv_profile_email.setText(model.getEmail());
                    firebaseStorage.getReference(model.getPath()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Glide.with(imv_profile.getContext())
                                    .load(uri)
                                    .into(imv_profile);
                        }
                    });
                }
            }
        });

        PostContentAdapter adapter = new PostContentAdapter(postReferenceModels);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,true);
        rv_profile_content.setLayoutManager(layoutManager);
        rv_profile_content.setAdapter(adapter);

        databaseReference.orderByChild("timestamp").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                PostReferenceModel model1 = snapshot.getValue(PostReferenceModel.class);
                postReferenceModels.add(model1);
                keys.add(snapshot.getKey());
                adapter.notifyItemInserted(postReferenceModels.size() - 1);
                tv_profile_posts.setText(String.valueOf(postReferenceModels.size()));
                rv_profile_content.smoothScrollToPosition(postReferenceModels.size() - 1);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                int position = keys.indexOf(snapshot.getKey());
                postReferenceModels.remove(position);
                keys.remove(position);
                adapter.notifyItemRemoved(position);
                tv_profile_posts.setText(String.valueOf(postReferenceModels.size()));
                if (postReferenceModels.size() > 0) {
                    rv_profile_content.smoothScrollToPosition(postReferenceModels.size() - 1);
                }
                rv_profile_content.invalidate();
                rv_profile_content.setAdapter(adapter);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View item =  inflater.inflate(R.layout.fragment_profile, container, false);

        cl_profile = item.findViewById(R.id.cl_profile);

        cv_profile_image = item.findViewById(R.id.cv_profile_image);
        imv_profile = item.findViewById(R.id.imv_profile);

        rv_profile_content = item.findViewById(R.id.rv_profile_content);

        tv_profile_posts = item.findViewById(R.id.tv_profile_posts);
        tv_profile_uname = item.findViewById(R.id.tv_profile_uname);
        tv_profile_email = item.findViewById(R.id.tv_profile_email);

        mAuth = FirebaseAuth.getInstance();

        firebaseFirestore = FirebaseFirestore.getInstance();
        collectionReference = firebaseFirestore.collection("Profiles");

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference("Profiles");

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users").child(mAuth.getCurrentUser().getUid());




        return item;

    }

    private void logoutUser() {
        mAuth.signOut(); // Logs out the user

        // Show toast message
        Toast.makeText(getContext(), "Logged Out Successfully", Toast.LENGTH_SHORT).show();

        // Redirect to SignInActivity
        Intent intent = new Intent(getContext(), SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}