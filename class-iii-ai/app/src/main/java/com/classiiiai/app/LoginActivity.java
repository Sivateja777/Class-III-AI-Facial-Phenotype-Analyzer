package com.classiiiai.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.FirebaseException;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.annotation.NonNull;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private Button btnGoogleSignIn;
    private ProgressBar pbLoading;
    private String selectedRole = "patient";
    
    private CredentialManager credentialManager;
    private ExecutorService executor;
    private static final int RC_SIGN_IN = 9001;
    
    private TextInputEditText etUsername;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etLicense;
    
    private TextInputLayout tilUsername;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    
    private Button btnSignIn;
    private Button btnRegister;
    private android.widget.CheckBox cbPrivacyConsent;
    

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        credentialManager = CredentialManager.create(this);
        executor = Executors.newSingleThreadExecutor();
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        pbLoading = findViewById(R.id.pbLoading);
        TextView tvLoginTitle = findViewById(R.id.tvLoginTitle);
        TextView tvLoginSubtitle = findViewById(R.id.tvLoginSubtitle);
        TextInputLayout tilLicense = findViewById(R.id.tilLicense);
        
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etLicense = findViewById(R.id.etLicense);
        etPassword = findViewById(R.id.etPassword);
        
        tilUsername = findViewById(R.id.tilUsername);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        
        btnSignIn = findViewById(R.id.btnSignIn);
        btnRegister = findViewById(R.id.btnRegister);
        cbPrivacyConsent = findViewById(R.id.cbPrivacyConsent);

        selectedRole = getIntent().getStringExtra("ROLE");
        if (selectedRole == null) selectedRole = "patient";

        if (selectedRole.equals("doctor")) {
            tvLoginTitle.setText("Doctor Login");
            tvLoginSubtitle.setText("Secure clinician access with license verification");
            tilLicense.setVisibility(View.VISIBLE);
        } else {
            tvLoginTitle.setText("Patient Login");
            tvLoginSubtitle.setText("Access personal analysis and report history");
        }

        btnGoogleSignIn.setOnClickListener(v -> performGoogleSignIn());
        btnSignIn.setOnClickListener(v -> loginWithEmail());
        btnRegister.setOnClickListener(v -> registerWithEmail());

        TextView btnSkipLogin = findViewById(R.id.btnSkipLogin);
        if (btnSkipLogin != null) {
            btnSkipLogin.setVisibility(View.GONE);
        }
    }
    
    private void loginWithEmail() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and Password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("doctor".equals(selectedRole)) {
            String licenseStr = etLicense.getText() != null ? etLicense.getText().toString().trim() : "";
            if (licenseStr.isEmpty() || !licenseStr.matches("^[A-Za-z0-9-]{5,20}$")) {
                Toast.makeText(this, "Invalid Medical License format.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        pbLoading.setVisibility(View.VISIBLE);
        btnSignIn.setEnabled(false);
        btnRegister.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    saveToDatabase(email, username, "", selectedRole, "");
                } else {
                    pbLoading.setVisibility(View.GONE);
                    btnSignIn.setEnabled(true);
                    btnRegister.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
    }

    private void registerWithEmail() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username, Email, and Password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cbPrivacyConsent.isChecked()) {
            Toast.makeText(this, "You must agree to the Data Privacy & GDPR Policy to register.", Toast.LENGTH_LONG).show();
            return;
        }

        if ("doctor".equals(selectedRole)) {
            String licenseStr = etLicense.getText() != null ? etLicense.getText().toString().trim() : "";
            if (licenseStr.isEmpty() || !licenseStr.matches("^[A-Za-z0-9-]{5,20}$")) {
                Toast.makeText(this, "Invalid Medical License format.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        pbLoading.setVisibility(View.VISIBLE);
        btnSignIn.setEnabled(false);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    saveToDatabase(email, username, "", selectedRole, "");
                } else {
                    pbLoading.setVisibility(View.GONE);
                    btnSignIn.setEnabled(true);
                    btnRegister.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
    }

    private void performGoogleSignIn() {
        if ("doctor".equals(selectedRole)) {
            String licenseStr = etLicense.getText() != null ? etLicense.getText().toString().trim() : "";
            if (licenseStr.isEmpty() || !licenseStr.matches("^[A-Za-z0-9-]{5,20}$")) {
                Toast.makeText(this, "Invalid Medical License format. Must be 5-20 alphanumeric characters.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (!cbPrivacyConsent.isChecked()) {
            Toast.makeText(this, "You must agree to the Data Privacy & GDPR Policy.", Toast.LENGTH_LONG).show();
            return;
        }
        
        btnGoogleSignIn.setEnabled(false);
        pbLoading.setVisibility(View.VISIBLE);

        String webClientId = getString(R.string.default_web_client_id);
        if (webClientId.equals("YOUR_WEB_CLIENT_ID_HERE")) {
            Toast.makeText(this, "ERROR: Missing Web Client ID in strings.xml", Toast.LENGTH_LONG).show();
            pbLoading.setVisibility(View.GONE);
            btnGoogleSignIn.setEnabled(true);
            return;
        }

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(this, request, null, executor, 
            new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
            @Override
            public void onResult(GetCredentialResponse result) {
                handleSignIn(result);
            }

            @Override
            public void onError(GetCredentialException e) {
                runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    btnGoogleSignIn.setEnabled(true);
                    
                    if (e instanceof androidx.credentials.exceptions.NoCredentialException) {
                        Toast.makeText(LoginActivity.this, "Using Fallback Google Sign-In...", Toast.LENGTH_SHORT).show();
                        launchFallbackGoogleSignIn();
                    } else {
                        Toast.makeText(LoginActivity.this, "Sign in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void launchFallbackGoogleSignIn() {
        String webClientId = getString(R.string.default_web_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String profilePic = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";
                    saveToDatabase(account.getEmail(), account.getDisplayName(), profilePic, selectedRole, "");
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Fallback Sign In Failed: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
                pbLoading.setVisibility(View.GONE);
                btnGoogleSignIn.setEnabled(true);
            }
        }
    }

    private void handleSignIn(GetCredentialResponse result) {
        try {
            if (result.getCredential() instanceof CustomCredential) {
                CustomCredential credential = (CustomCredential) result.getCredential();
                if (credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                    GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                    
                    String email = googleIdTokenCredential.getId();
                    String displayName = googleIdTokenCredential.getDisplayName();
                    String profilePictureUri = googleIdTokenCredential.getProfilePictureUri() != null ? googleIdTokenCredential.getProfilePictureUri().toString() : "";
                    
                    saveToDatabase(email, displayName, profilePictureUri, selectedRole, "");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting Google credentials", e);
            runOnUiThread(() -> {
                pbLoading.setVisibility(View.GONE);
                btnGoogleSignIn.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Error parsing Google Token", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void saveToDatabase(String email, String name, String profilePic, String role, String mobile) {
        executor.execute(() -> {
            com.classiiiai.app.data.AppDatabase db = com.classiiiai.app.data.AppDatabase.getDatabase(this);
            com.classiiiai.app.data.User user = new com.classiiiai.app.data.User(email, name, profilePic, role, System.currentTimeMillis());
            if (!mobile.isEmpty()) {
                user.phoneNumber = mobile;
            }
            db.userDao().insertUser(user);
            
            String emailKey = email.replace(".", "_").replace("@", "_");
            com.classiiiai.app.network.FirebaseApiService api = com.classiiiai.app.network.FirebaseClient.getClient().create(com.classiiiai.app.network.FirebaseApiService.class);
            
            api.saveUser(emailKey, user).enqueue(new retrofit2.Callback<com.classiiiai.app.data.User>() {
                @Override
                public void onResponse(retrofit2.Call<com.classiiiai.app.data.User> call, retrofit2.Response<com.classiiiai.app.data.User> response) {
                    runOnUiThread(() -> {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onFailure(retrofit2.Call<com.classiiiai.app.data.User> call, Throwable t) {
                    runOnUiThread(() -> {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Welcome " + name + " (Offline)", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }
            });
        });
    }
}
