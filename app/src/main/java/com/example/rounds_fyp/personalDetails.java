package com.example.rounds_fyp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class personalDetails extends Fragment {

    private TextView userName, memberSince;
    private TextInputEditText emailInput, phoneInput, nationalIdInput, ageInput;
    private Button saveButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_personal_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();
        }

        initializeViews(view);

        emailInput.setEnabled(false);
        emailInput.setFocusable(false);
        emailInput.setClickable(false);

        loadUserData();

        saveButton.setOnClickListener(v -> saveUserData());
    }

    private void initializeViews(View view) {
        userName = view.findViewById(R.id.userName);
        memberSince = view.findViewById(R.id.memberSince);
        emailInput = view.findViewById(R.id.emailInput);
        phoneInput = view.findViewById(R.id.phoneInput);
        nationalIdInput = view.findViewById(R.id.nationalIdInput);
        ageInput = view.findViewById(R.id.ageInput);
        saveButton = view.findViewById(R.id.saveButton);
    }

    private void loadUserData() {
        if (currentUser == null) {
            showToast("No user signed in");
            return;
        }

        emailInput.setText(currentUser.getEmail());

        DocumentReference userRef = firestore.collection("users").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    String fullName = doc.getString("fullName");
                    String phone = doc.getString("phone");
                    String nationalId = doc.getString("nationalId");
                    String age = doc.getString("age");
                    Long createdAt = doc.getLong("createdAt");

                    if (fullName != null && !fullName.isEmpty()) userName.setText(fullName);
                    if (phone != null) phoneInput.setText(phone);
                    if (nationalId != null) nationalIdInput.setText(nationalId);
                    if (age != null) ageInput.setText(age);

                    if (createdAt != null) {
                        Date date = new Date(createdAt);
                        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                        memberSince.setText("Member since " + sdf.format(date));
                    } else {
                        memberSince.setText("Member since -");
                    }
                } else {
                    createNewUserDocument();
                }
            } else {
                showToast("Error loading data: " + task.getException().getMessage());
            }
        });
    }

    private void createNewUserDocument() {
        if (currentUser == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", currentUser.getEmail());
        userData.put("createdAt", System.currentTimeMillis());

        String defaultName = "User";
        String displayName = currentUser.getDisplayName();

        if (displayName != null && !displayName.isEmpty()) {
            userData.put("fullName", displayName);
            userName.setText(displayName);
        } else {
            userData.put("fullName", defaultName);
            userName.setText(defaultName);
        }

        firestore.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                    memberSince.setText("Member since " + sdf.format(new Date()));
                })
                .addOnFailureListener(e -> showToast("Error creating user: " + e.getMessage()));
    }

    private void saveUserData() {
        if (currentUser == null) {
            showToast("No user signed in");
            return;
        }

        String phone = phoneInput.getText().toString().trim();
        String nationalId = nationalIdInput.getText().toString().trim();
        String age = ageInput.getText().toString().trim();

        if (nationalId.isEmpty()) {
            showToast("National ID cannot be empty");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", phone);
        updates.put("nationalId", nationalId);
        updates.put("age", age);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> showToast("Details updated"))
                .addOnFailureListener(e -> showToast("Error saving: " + e.getMessage()));
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
