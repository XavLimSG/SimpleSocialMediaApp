package com.example.simplesocialmediaapp;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.simplesocialmediaapp.Adapters.PostContentAdapter;
import com.example.simplesocialmediaapp.Models.PostReferenceModel;
import com.example.simplesocialmediaapp.Models.ProfileModel;
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

import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {

    String UserId;

    CardView cv_user_profile_image;
    ImageView imv_user_profile;

    ProfileModel model;

    TextView tv_user_profile_posts, tv_user_profile_followers, tv_user_profile_following, tv_user_profile_uname, tv_user_profile_email;

    RecyclerView rv_user_profile_content;

    ArrayList<PostReferenceModel> postReferenceModels = new ArrayList<>();
    ArrayList<String> keys = new ArrayList<>();

    FirebaseAuth mAuth;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    FirebaseFirestore firebaseFirestore;

    CollectionReference collectionReference;

    FirebaseStorage firebaseStorage;

    StorageReference storageReference;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        UserId = getIntent().getStringExtra("uid");

        initvar();

        collectionReference.document(UserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
               if(task.isSuccessful())
               {
                   model = task.getResult().toObject(ProfileModel.class);
                   tv_user_profile_uname.setText(model.getUsername());
                   tv_user_profile_email.setText(model.getEmail());
                   getSupportActionBar().setTitle(model.getUsername());
                   firebaseStorage.getReference(model.getPath()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                       @Override
                       public void onSuccess(Uri uri) {
                           Glide.with(imv_user_profile.getContext())
                                   .load(uri)
                                   .into(imv_user_profile);
                       }
                   });
               }
            }
        });

        PostContentAdapter adapter = new PostContentAdapter(postReferenceModels);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        rv_user_profile_content.setLayoutManager(layoutManager);
        rv_user_profile_content.setAdapter(adapter);

        databaseReference.orderByChild("timestamp").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                PostReferenceModel model1 = snapshot.getValue(PostReferenceModel.class);
                postReferenceModels.add(model1);
                keys.add(snapshot.getKey());
                adapter.notifyItemInserted(postReferenceModels.size() -1 );
                tv_user_profile_posts.setText(String.valueOf(postReferenceModels.size()));
                rv_user_profile_content.smoothScrollToPosition(postReferenceModels.size() -1);


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
                if (postReferenceModels.size() > 0)
                {
                    rv_user_profile_content.smoothScrollToPosition(postReferenceModels.size() -1);
                }
                rv_user_profile_content.invalidate();
                rv_user_profile_content.setAdapter(adapter);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

    }

    void initvar()
    {
        cv_user_profile_image = findViewById(R.id.cv_user_profile_image);
        imv_user_profile = findViewById(R.id.imv_user_profile);

        tv_user_profile_posts = findViewById(R.id.tv_user_profile_posts);
        tv_user_profile_followers = findViewById(R.id.tv_user_profile_followers);
        tv_user_profile_following = findViewById(R.id.tv_user_profile_following);
        tv_user_profile_uname = findViewById(R.id.tv_user_profile_uname);
        tv_user_profile_email = findViewById(R.id.tv_user_profile_email);

        rv_user_profile_content = findViewById(R.id.rv_user_profile_content);

        mAuth = FirebaseAuth.getInstance();

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users").child(UserId);

        firebaseFirestore = FirebaseFirestore.getInstance();
        collectionReference = firebaseFirestore.collection("Profiles");

        firebaseStorage = FirebaseStorage.getInstance();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}