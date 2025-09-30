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

public class login extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private TextInputLayout emailLayout, passwordLayout;
    private Button buttonLogin;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView textViewSignUpNow, textViewForgotPassword;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        initializeUI();

        // Set click listeners
        setClickListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is already signed in
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
            editTextEmail = findViewById(R.id.email);
            editTextPassword = findViewById(R.id.password);

            // TextInputLayouts for validation
            emailLayout = findViewById(R.id.emailLayout);
            passwordLayout = findViewById(R.id.passwordLayout);

            // Buttons and other views
            buttonLogin = findViewById(R.id.buttonLogIn);
            progressBar = findViewById(R.id.progressBar);
            textViewSignUpNow = findViewById(R.id.signUpNow);
            textViewForgotPassword = findViewById(R.id.forgotPassword);

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
        // Sign up text click listener
        if (textViewSignUpNow != null) {
            textViewSignUpNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    navigateToSignUp();
                }
            });
        }

        // Forgot password text click listener
        if (textViewForgotPassword != null) {
            textViewForgotPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showForgotPasswordDialog();
                }
            });
        }

        // Login button click listener
        if (buttonLogin != null) {
            buttonLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (validateInputs()) {
                        loginUser();
                    }
                }
            });
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Null checks for UI components
        if (emailLayout == null || passwordLayout == null ||
                editTextEmail == null || editTextPassword == null) {
            Toast.makeText(this, "UI components not properly initialized", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Reset errors
        emailLayout.setError(null);
        passwordLayout.setError(null);

        // Get input values
        String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
        String password = editTextPassword.getText() != null ? editTextPassword.getText().toString() : "";

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
        }

        return isValid;
    }

    private void loginUser() {
        // Null checks
        if (mAuth == null || progressBar == null) {
            Toast.makeText(this, "Firebase services not properly initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);

        // Get input values
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        // Authenticate user with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Hide progress bar regardless of result
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(login.this, "Login successful!",
                                    Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        } else {
                            // If sign in fails, display a message to the user
                            Log.w(TAG, "signInWithEmail:failure", task.getException());

                            // Show appropriate error message
                            if (task.getException() != null && task.getException().getMessage() != null) {
                                String errorMessage = task.getException().getMessage();
                                if (errorMessage.contains("password is invalid") ||
                                        errorMessage.contains("no user record")) {
                                    Toast.makeText(login.this, "Invalid email or password",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(login.this, "Authentication failed: " + errorMessage,
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(login.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void showForgotPasswordDialog() {
        // Implementation for forgot password functionality
        // You can show a dialog to enter email address for password reset
        // For now, just show a toast message
        Toast.makeText(this, "Forgot password functionality will be implemented soon",
                Toast.LENGTH_SHORT).show();

        // Complete implementation would look like this:
        /*
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        // Set up the input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Enter your email address");
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = input.getText().toString().trim();
                if (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    sendPasswordResetEmail(email);
                } else {
                    Toast.makeText(login.this, "Please enter a valid email address",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
        */
    }

    private void sendPasswordResetEmail(String email) {
        if (mAuth != null) {
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(login.this, "Password reset email sent",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(login.this, "Failed to send reset email. " +
                                                (task.getException() != null ? task.getException().getMessage() : ""),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void navigateToSignUp() {
        Intent intent = new Intent(login.this, signUp.class);
        startActivity(intent);
        finish();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(login.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}