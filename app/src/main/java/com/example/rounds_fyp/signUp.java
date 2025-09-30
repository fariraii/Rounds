package com.example.rounds_fyp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class signUp extends AppCompatActivity {

    private TextInputEditText editTextFullName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private TextInputLayout fullNameLayout, emailLayout, passwordLayout, confirmPasswordLayout;
    private Button buttonSignUp;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView textViewLoginNow;
    private static final String TAG = "SignUpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firestore: " + e.getMessage());
            Toast.makeText(this, "Error initializing Firebase services. Check your connection.", Toast.LENGTH_LONG).show();
        }

        // Initialize UI components
        initializeUI();

        // Set click listeners
        setClickListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is already signed in (non-null)
        if (mAuth != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if(currentUser != null) {
                navigateToMainActivity();
            }
        }
    }

    private void initializeUI() {
        try {
            // EditText fields
            editTextFullName = findViewById(R.id.fullName);
            editTextEmail = findViewById(R.id.email);
            editTextPassword = findViewById(R.id.password);
            editTextConfirmPassword = findViewById(R.id.confirmPassword);

            // TextInputLayouts for validation
            fullNameLayout = findViewById(R.id.fullNameLayout);
            emailLayout = findViewById(R.id.emailLayout);
            passwordLayout = findViewById(R.id.passwordLayout);
            confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

            // Buttons and other views
            buttonSignUp = findViewById(R.id.buttonSignUp);
            progressBar = findViewById(R.id.progressBar);
            textViewLoginNow = findViewById(R.id.loginNow);

            // Make sure progress bar is initially hidden
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI components: " + e.getMessage());
            Toast.makeText(this, "Error setting up the page. Please restart the app.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setClickListeners() {
        // Login text click listener
        if (textViewLoginNow != null) {
            textViewLoginNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    navigateToLogin();
                }
            });
        }

        // Sign up button click listener
        if (buttonSignUp != null) {
            buttonSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (validateInputs()) {
                        registerUser();
                    }
                }
            });
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Null checks for all UI components
        if (fullNameLayout == null || emailLayout == null || passwordLayout == null ||
                confirmPasswordLayout == null || editTextFullName == null || editTextEmail == null ||
                editTextPassword == null || editTextConfirmPassword == null) {
            Toast.makeText(this, "UI components not properly initialized", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Reset errors
        fullNameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);

        // Get input values
        String fullName = editTextFullName.getText() != null ? editTextFullName.getText().toString().trim() : "";
        String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
        String password = editTextPassword.getText() != null ? editTextPassword.getText().toString() : "";
        String confirmPassword = editTextConfirmPassword.getText() != null ? editTextConfirmPassword.getText().toString() : "";

        // Validate full name
        if (TextUtils.isEmpty(fullName)) {
            fullNameLayout.setError("Full name is required");
            isValid = false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Please enter a valid email address");
            isValid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            isValid = false;
        }

        return isValid;
    }

    private void registerUser() {
        // Null checks
        if (mAuth == null || progressBar == null) {
            Toast.makeText(this, "Firebase services not properly initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);

        // Get input values
        final String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        // Create user with Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign up success
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Store additional user data in Firestore
                            if (user != null && db != null) {
                                storeUserData(user.getUid(), fullName, user.getEmail());
                            } else {
                                // Hide progress bar
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                                Toast.makeText(signUp.this, "Registration successful but failed to get user data",
                                        Toast.LENGTH_SHORT).show();
                                navigateToMainActivity();
                            }
                        } else {
                            // If sign up fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());

                            // Hide progress bar
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }

                            // Show appropriate error message
                            if (task.getException() != null && task.getException().getMessage() != null) {
                                if (task.getException().getMessage().contains("email address is already in use")) {
                                    if (emailLayout != null) {
                                        emailLayout.setError("Email is already registered");
                                    }
                                } else {
                                    Toast.makeText(signUp.this, "Authentication failed: " + task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(signUp.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void storeUserData(String userId, String fullName, String email) {
        // Create a new user with data
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", fullName);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());

        // Add a new document with the user ID
        db.collection("users").document(userId)
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Hide progress bar
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        if (task.isSuccessful()) {
                            // Data stored successfully
                            Toast.makeText(signUp.this, "Registration successful!",
                                    Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        } else {
                            // Failed to store data
                            Log.w(TAG, "Error adding user data", task.getException());
                            Toast.makeText(signUp.this, "Registration successful but failed to store user data",
                                    Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Ensure progress bar is hidden even if Firestore operation fails
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Log.e(TAG, "Firestore error: " + e.getMessage());
                    Toast.makeText(signUp.this, "Failed to store user data.", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(signUp.this, login.class);
        startActivity(intent);
        finish();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(signUp.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}