package com.classiiiai.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.classiiiai.app.data.AppDatabase;
import com.classiiiai.app.data.User;
import com.classiiiai.app.data.UserDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnboardingActivity extends AppCompatActivity {

    private EditText etDob;
    private EditText etLicense;
    private LinearLayout llLicenseContainer;
    private Button btnCompleteProfile;
    private ExecutorService executor;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        etDob = findViewById(R.id.etDob);
        etLicense = findViewById(R.id.etLicense);
        llLicenseContainer = findViewById(R.id.llLicenseContainer);
        btnCompleteProfile = findViewById(R.id.btnCompleteProfile);

        executor = Executors.newSingleThreadExecutor();
        
        loadUserAndConfigureUI();

        btnCompleteProfile.setOnClickListener(v -> saveProfileAndContinue());
    }

    private void loadUserAndConfigureUI() {
        executor.execute(() -> {
            currentUser = AppDatabase.getDatabase(this).userDao().getLastLoggedInUser();
            if (currentUser != null) {
                runOnUiThread(() -> {
                    if ("doctor".equals(currentUser.role)) {
                        llLicenseContainer.setVisibility(View.VISIBLE);
                        if (currentUser.medicalLicenseNumber != null && !currentUser.medicalLicenseNumber.isEmpty()) {
                            etLicense.setText(currentUser.medicalLicenseNumber);
                        }
                    }
                });
            }
        });
    }

    private void saveProfileAndContinue() {
        if (currentUser == null) return;

        String dob = etDob.getText().toString().trim();

        if (dob.isEmpty()) {
            Toast.makeText(this, "Please provide your Date of Birth", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if ("doctor".equals(currentUser.role)) {
            String licenseStr = etLicense.getText().toString().trim();
            if (licenseStr.isEmpty() || !licenseStr.matches("^[A-Za-z0-9-]{5,20}$")) {
                Toast.makeText(this, "Please provide a valid Medical License Number (5-20 characters)", Toast.LENGTH_LONG).show();
                return;
            }
            currentUser.medicalLicenseNumber = licenseStr;
        }

        currentUser.dateOfBirth = dob;
        currentUser.isProfileComplete = true;

        executor.execute(() -> {
            AppDatabase.getDatabase(this).userDao().updateUser(currentUser);
            
            // Sync to Firestore
            String emailKey = currentUser.email.replace(".", "_").replace("@", "_");
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("dateOfBirth", dob);
            updates.put("isProfileComplete", true);
            if ("doctor".equals(currentUser.role)) {
                updates.put("medicalLicenseNumber", currentUser.medicalLicenseNumber);
            }
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(emailKey).update(updates);
            
            runOnUiThread(() -> {
                Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }
}
