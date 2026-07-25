package com.classiiiai.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Trigger silent background sync to keep Room DB updated with Firebase data
        com.classiiiai.app.network.SyncManager.syncReportsFromFirebase(this);

        Executors.newSingleThreadExecutor().execute(() -> {
            com.classiiiai.app.data.User user = com.classiiiai.app.data.AppDatabase.getDatabase(this).userDao().getLastLoggedInUser();
            if (user != null && !user.isProfileComplete) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity.this, OnboardingActivity.class);
                    startActivity(intent);
                    finish();
                });
                return;
            }

            String userRole = (user != null && user.role != null) ? user.role : "doctor";

            runOnUiThread(() -> setupNavigation(userRole, savedInstanceState));
        });
    }

    private void setupNavigation(String userRole, Bundle savedInstanceState) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if (userRole.equals("patient")) {
            bottomNav.inflateMenu(R.menu.bottom_nav_menu_patient);
            bottomNav.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_patient_home) selectedFragment = new PatientHomeFragment();
                else if (itemId == R.id.nav_patient_reports) selectedFragment = new PatientReportsFragment();
                else if (itemId == R.id.nav_patient_visits) selectedFragment = new PatientVisitsFragment();
                else if (itemId == R.id.nav_patient_simulation) selectedFragment = new SimulationFragment();
                else if (itemId == R.id.nav_patient_profile) selectedFragment = new ProfileFragment();

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                }
                return true;
            });
            if (savedInstanceState == null) bottomNav.setSelectedItemId(R.id.nav_patient_home);
        } else if (userRole.equals("admin")) {
            bottomNav.inflateMenu(R.menu.bottom_nav_menu_admin);
            bottomNav.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = new AdminFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                return true;
            });
            if (savedInstanceState == null) bottomNav.setSelectedItemId(R.id.nav_admin_dashboard);
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_menu);
            bottomNav.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) selectedFragment = new HomeFragment();
                else if (itemId == R.id.nav_patients) selectedFragment = new PatientsFragment();
                else if (itemId == R.id.nav_analysis) selectedFragment = new AnalysisFragment();
                else if (itemId == R.id.nav_reports) selectedFragment = new ReportsFragment();
                else if (itemId == R.id.nav_visits) selectedFragment = new PatientVisitsFragment();

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                }
                return true;
            });
            if (savedInstanceState == null) bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}
