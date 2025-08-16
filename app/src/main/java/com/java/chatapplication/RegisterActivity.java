package com.java.chatapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.java.chatapplication.models.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword;
    private ImageView ivProfile;
    private Uri imageUri;

    private FirebaseAuth auth;
    private StorageReference storageRef;
    private DatabaseReference dbRef;
    private GoogleSignInClient mGoogleSignInClient;

    private ProgressDialog progressDialog;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (data != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    ivProfile.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivProfile = findViewById(R.id.ivProfile);
        Button btnRegister = findViewById(R.id.btnRegister);
        SignInButton btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        Button login = findViewById(R.id.login);

        login.setOnClickListener(view -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));

        auth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");
        dbRef = FirebaseDatabase.getInstance().getReference("users");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering...");
        progressDialog.setCancelable(false);

        // Google Sign-In configuration
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // from google-services.json
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        ivProfile.setOnClickListener(v -> imagePicker.launch("image/*"));
        btnRegister.setOnClickListener(v -> registerUserWithEmail());
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());



    }

    private void registerUserWithEmail() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Enter username");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        progressDialog.show();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = Objects.requireNonNull(authResult.getUser()).getUid();
                    if (imageUri != null) {
                        uploadProfileImage(uid, username, email);
                    } else {
                        saveUserToRealtimeDB(uid, username, email, null);
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadProfileImage(String uid, String username, String email) {
        StorageReference fileRef = storageRef.child(uid + ".jpg");
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveUserToRealtimeDB(uid, username, email, uri.toString()))
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserToRealtimeDB(String uid, String username, String email, String profileImageUrl) {
        User user = new User(uid, username, email, profileImageUrl);
        user.setStatus("online");
        // Set lastChanged to ServerValue.TIMESTAMP using a Map for Firebase
        Map<String, Object> lastChangedMap = new HashMap<>();
        lastChangedMap.put("lastChanged", ServerValue.TIMESTAMP);
        user.setLastChanged(lastChangedMap.get("lastChanged"));


        dbRef.child(uid).setValue(user)
                .addOnSuccessListener(unused -> {
                    DatabaseReference statusRef = dbRef.child(uid).child("status");
                    DatabaseReference lastChangedRef = dbRef.child(uid).child("lastChanged");

                    // Set offline status on disconnect
                    statusRef.onDisconnect().setValue("offline");
                    lastChangedRef.onDisconnect().setValue(ServerValue.TIMESTAMP);

                    progressDialog.dismiss();
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void signInWithGoogle() {
        progressDialog.show();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Note: User name, email, and photo might be null, so handle that safely
                            String username = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User";
                            String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                            String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null;

                            saveUserToRealtimeDB(firebaseUser.getUid(), username, email, photoUrl);
                        }
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
