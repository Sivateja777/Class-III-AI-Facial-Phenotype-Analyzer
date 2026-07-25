package com.classiiiai.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.Executors;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Auto-Login Check (Temporarily disabled for testing login UI)
        /*
        Executors.newSingleThreadExecutor().execute(() -> {
            com.classiiiai.app.data.AppDatabase db = com.classiiiai.app.data.AppDatabase.getDatabase(this);
            com.classiiiai.app.data.User user = db.userDao().getLastLoggedInUser();
            if (user != null) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(RoleSelectionActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
        });
        */

        setContentView(R.layout.activity_role_selection);

        CheckBox cbConsent = findViewById(R.id.cbConsent);
        LinearLayout llRoleDoctor = findViewById(R.id.llRoleDoctor);
        LinearLayout llRolePatient = findViewById(R.id.llRolePatient);

        llRoleDoctor.setOnClickListener(v -> {
            if (!cbConsent.isChecked()) {
                Toast.makeText(this, "Please accept the consent disclaimer first.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("ROLE", "doctor");
            startActivity(intent);
        });

        llRolePatient.setOnClickListener(v -> {
            if (!cbConsent.isChecked()) {
                Toast.makeText(this, "Please accept the consent disclaimer first.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("ROLE", "patient");
            startActivity(intent);
        });
    }
}
