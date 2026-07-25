package com.classiiiai.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                com.classiiiai.app.data.User user = com.classiiiai.app.data.AppDatabase.getDatabase(SplashActivity.this).userDao().getLastLoggedInUser();
                
                runOnUiThread(() -> {
                    Intent intent;
                    if (user != null) {
                        intent = new Intent(SplashActivity.this, MainActivity.class);
                    } else {
                        intent = new Intent(SplashActivity.this, RoleSelectionActivity.class);
                    }
                    startActivity(intent);
                    finish();
                });
            });
        }, 1500);
    }
}
