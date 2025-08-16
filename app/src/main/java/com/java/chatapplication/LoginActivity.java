package com.java.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText emailEditText, passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        Button emailLoginButton = findViewById(R.id.emailLoginButton);

        emailLoginButton.setOnClickListener(v -> loginWithEmail());

        // If already logged in → go to HomeActivity
        if (mAuth.getCurrentUser() != null) {
            goToHome();
        }
    }

    private void loginWithEmail() {
        String email = emailEditText.getText().toString().trim();
        String pass = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show();
                    goToHome();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "No account found, please register", Toast.LENGTH_SHORT).show();
                    goToRegister();
                });
    }

    private void goToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void goToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }
}
