package com.classiiiai.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.fragment.app.Fragment;

import com.classiiiai.app.data.AppDatabase;
import com.classiiiai.app.data.User;
import com.classiiiai.app.data.UserDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private EditText etDisplayName, etEmail, etPhone, etDob;
    private TextView tvRole;
    private ImageView ivProfileImage;
    private Button btnSaveChanges, btnLogout;
    private User currentUser;
    private ExecutorService executor;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etDisplayName = view.findViewById(R.id.etDisplayName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etDob = view.findViewById(R.id.etDob);
        tvRole = view.findViewById(R.id.tvRole);
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        btnLogout = view.findViewById(R.id.btnLogout);

        executor = Executors.newSingleThreadExecutor();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            ivProfileImage.setImageURI(selectedImageUri);
                        }
                    }
                }
        );

        loadUserProfile();

        ivProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnSaveChanges.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserProfile() {
        executor.execute(() -> {
            UserDao userDao = AppDatabase.getDatabase(requireContext()).userDao();
            currentUser = userDao.getLastLoggedInUser();

            if (currentUser != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    etDisplayName.setText(currentUser.displayName != null ? currentUser.displayName : "");
                    etEmail.setText(currentUser.email != null ? currentUser.email : "");
                    etPhone.setText(currentUser.phoneNumber != null ? currentUser.phoneNumber : "");
                    etDob.setText(currentUser.dateOfBirth != null ? currentUser.dateOfBirth : "");
                    
                    if ("patient".equalsIgnoreCase(currentUser.role)) {
                        tvRole.setVisibility(View.GONE);
                    } else {
                        tvRole.setVisibility(View.VISIBLE);
                        tvRole.setText(currentUser.role);
                    }
                    
                    if (currentUser.profilePictureUrl != null && !currentUser.profilePictureUrl.isEmpty()) {
                        try {
                            ivProfileImage.setImageURI(Uri.parse(currentUser.profilePictureUrl));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void saveProfile() {
        if (currentUser == null) return;
        
        String newName = etDisplayName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newDob = etDob.getText().toString().trim();
        
        if (newName.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(requireContext(), "Name and Email cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUser.displayName = newName;
        currentUser.email = newEmail;
        currentUser.phoneNumber = newPhone;
        currentUser.dateOfBirth = newDob;
        if (selectedImageUri != null) {
            uploadImageToFirebaseStorage();
        } else {
            saveToLocalDatabase();
        }
    }
    
    private void uploadImageToFirebaseStorage() {
        if (getActivity() == null) return;
        Toast.makeText(requireContext(), "Uploading Profile Image...", Toast.LENGTH_SHORT).show();
        
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference profileRef = storageRef.child("profile_pictures/" + currentUser.email + "_" + System.currentTimeMillis() + ".jpg");
        
        profileRef.putFile(selectedImageUri)
            .addOnSuccessListener(taskSnapshot -> {
                profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    currentUser.profilePictureUrl = uri.toString();
                    saveToLocalDatabase();
                });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // Save anyway without the new image
                saveToLocalDatabase();
            });
    }

    private void saveToLocalDatabase() {

        executor.execute(() -> {
            UserDao userDao = AppDatabase.getDatabase(requireContext()).userDao();
            userDao.updateUser(currentUser);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void logout() {
        final android.content.Context ctx = getContext();
        if (ctx == null) return;
        
        executor.execute(() -> {
            try {
                AppDatabase.getDatabase(ctx).userDao().deleteAllUsers();

                CredentialManager credentialManager = CredentialManager.create(ctx);
                ClearCredentialStateRequest request = new ClearCredentialStateRequest();

                credentialManager.clearCredentialStateAsync(request, null, executor,
                        new androidx.credentials.CredentialManagerCallback<Void, androidx.credentials.exceptions.ClearCredentialException>() {
                            @Override
                            public void onResult(Void result) {
                                navigateToRoleSelection(ctx);
                            }
                            @Override
                            public void onError(androidx.credentials.exceptions.ClearCredentialException e) {
                                navigateToRoleSelection(ctx);
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
                navigateToRoleSelection(ctx);
            }
        });
    }

    private void navigateToRoleSelection(android.content.Context ctx) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Intent intent = new Intent(ctx, RoleSelectionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }
    }
}
