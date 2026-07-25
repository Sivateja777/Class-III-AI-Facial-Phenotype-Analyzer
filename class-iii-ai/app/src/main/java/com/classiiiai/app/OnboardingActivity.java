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
    private Button btnCompleteProfile;
    private ExecutorService executor;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        etDob = findViewById(R.id.etDob);
        btnCompleteProfile = findViewById(R.id.btnCompleteProfile);

        executor = Executors.newSingleThreadExecutor();
        
        loadUserAndConfigureUI();

        btnCompleteProfile.setOnClickListener(v -> saveProfileAndContinue());
    }

    private void loadUserAndConfigureUI() {
        executor.execute(() -> {
            currentUser = AppDatabase.getDatabase(this).userDao().getLastLoggedInUser();
        });
    }

    private void saveProfileAndContinue() {
        if (currentUser == null) return;

        String dob = etDob.getText().toString().trim();

        if (dob.isEmpty()) {
            Toast.makeText(this, "Please provide your Date of Birth", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser.dateOfBirth = dob;
        currentUser.isProfileComplete = true;

        executor.execute(() -> {
            AppDatabase.getDatabase(this).userDao().updateUser(currentUser);
            runOnUiThread(() -> {
                Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }
}
