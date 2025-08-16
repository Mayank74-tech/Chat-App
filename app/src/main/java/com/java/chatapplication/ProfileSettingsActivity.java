package com.java.chatapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileSettingsActivity extends AppCompatActivity {

    private ImageView profileImage;
    private EditText etUsername;
    private Button changePhoto, changeUsername, btnSignOut;

    private DatabaseReference userRef;
    private StorageReference storageRef;
    private FirebaseAuth auth;
    private Uri imageUri;

    private String uid;

    ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    profileImage.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        profileImage = findViewById(R.id.imgProfile);
        etUsername = findViewById(R.id.etUsername);
        changePhoto = findViewById(R.id.btnChangePhoto);
        changeUsername = findViewById(R.id.btnSaveUsername);
        btnSignOut = findViewById(R.id.btnSignOut);

        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");

        // Load user data
        loadUserData();

        // Change profile photo
        changePhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Save username or photo changes
        changeUsername.setOnClickListener(v -> saveChanges());

        // Sign out
        btnSignOut.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(ProfileSettingsActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String imageUrl = snapshot.child("imageUrl").getValue(String.class);

                    etUsername.setText(username);

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(ProfileSettingsActivity.this).load(imageUrl).into(profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileSettingsActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveChanges() {
        String username = etUsername.getText().toString().trim();
        if (username.isEmpty()) {
            etUsername.setError("Username required");
            return;
        }

        if (imageUri != null) {
            StorageReference fileRef = storageRef.child(uid + ".jpg");
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();
                                updateUserData(username, imageUrl);
                            }))
                    .addOnFailureListener(e -> Toast.makeText(ProfileSettingsActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show());
        } else {
            updateUserData(username, null);
        }
    }

    private void updateUserData(String username, String imageUrl) {
        if (imageUrl != null) {
            userRef.child("imageUrl").setValue(imageUrl);
        }
        userRef.child("username").setValue(username)
                .addOnSuccessListener(aVoid -> Toast.makeText(ProfileSettingsActivity.this, "Profile updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ProfileSettingsActivity.this, "Update failed", Toast.LENGTH_SHORT).show());
    }
}
