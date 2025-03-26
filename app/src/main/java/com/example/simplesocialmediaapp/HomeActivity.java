package com.example.simplesocialmediaapp;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.simplesocialmediaapp.Fragments.ChatsFragment;
import com.example.simplesocialmediaapp.Fragments.CirclesFragment;
import com.example.simplesocialmediaapp.Fragments.CreateFragment;
import com.example.simplesocialmediaapp.Fragments.HomeFragment;
import com.example.simplesocialmediaapp.Fragments.ProfileFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    TabLayout apptabs;
    ViewPager2 pager;
    ViewPagerFragmentAdapter adapter;
    boolean isAdmin = false; // Flag to differentiate teacher vs. student

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // For edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Read admin flag passed from SignInActivity
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);

        initvar();

        // If the user is not admin, remove the 3rd tab ("Create") from the TabLayout.
        // The "Create" tab is index 2 (0=Home,1=Circles,2=Create,3=Chats,4=Profile).
        if (!isAdmin && apptabs.getTabCount() > 2) {
            apptabs.removeTabAt(2);
        }

        // Rebuild the ViewPager adapter with the updated tab count and admin flag
        adapter = new ViewPagerFragmentAdapter(this, apptabs.getTabCount(), isAdmin);
        pager.setAdapter(adapter);

        settabs();

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                apptabs.selectTab(apptabs.getTabAt(position));
            }
        });
    }

    void initvar() {
        mAuth = FirebaseAuth.getInstance();
        apptabs = findViewById(R.id.apptabs);
        pager = findViewById(R.id.pager);
    }

    void settabs() {
        apptabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getIcon() != null) {
                    tab.getIcon().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
                }
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getIcon() != null) {
                    tab.getIcon().setColorFilter(Color.parseColor("#4F78D0"), PorterDuff.Mode.SRC_IN);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Optional: handle reselect
            }
        });

        if (apptabs.getTabAt(0) != null) {
            if (apptabs.getTabAt(0).getIcon() != null) {
                apptabs.getTabAt(0).getIcon().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
            }
            apptabs.selectTab(apptabs.getTabAt(0));
            pager.setCurrentItem(0);
        }
    }

    // Custom adapter that changes the number of fragments based on isAdmin
    public static class ViewPagerFragmentAdapter extends FragmentStateAdapter {

        int tabCount;
        boolean isAdmin;

        public ViewPagerFragmentAdapter(@NonNull FragmentActivity fragmentActivity, int tabCount, boolean isAdmin) {
            super(fragmentActivity);
            this.tabCount = tabCount;
            this.isAdmin = isAdmin;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (isAdmin) {
                // Teacher: All 5 tabs are shown (index: 0=Home,1=Circles,2=Create,3=Chats,4=Profile)
                switch (position) {
                    case 0: return new HomeFragment();
                    case 1: return new CirclesFragment();
                    case 2: return new CreateFragment();
                    case 3: return new ChatsFragment();
                    case 4: return new ProfileFragment();
                    default: return new HomeFragment();
                }
            } else {
                // Student: 4 tabs remain (0=Home,1=Circles,2=Chats,3=Profile)
                switch (position) {
                    case 0: return new HomeFragment();
                    case 1: return new CirclesFragment();
                    case 2: return new ChatsFragment();
                    case 3: return new ProfileFragment();
                    default: return new HomeFragment();
                }
            }
        }

        @Override
        public int getItemCount() {
            return tabCount;
        }
    }
}
