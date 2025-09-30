package com.example.rounds_fyp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class joinGroup extends Fragment {

    // UI components
    private TextInputEditText inviteCodeInput;
    private Button joinButton;
    private TextView errorTextView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_join_group, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI components
        inviteCodeInput = view.findViewById(R.id.inviteCodeInput);
        joinButton = view.findViewById(R.id.joinButton);
        errorTextView = view.findViewById(R.id.errorTextView);

        // Set up click listeners
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinGroup();
            }
        });

        return view;
    }

    private void joinGroup() {
        if (currentUser == null) {
            showError("You must be logged in to join a group");
            return;
        }

        String inviteCode = inviteCodeInput.getText().toString().trim();
        if (TextUtils.isEmpty(inviteCode)) {
            inviteCodeInput.setError("Please enter an invite code");
            return;
        }

        // Hide any previous error messages
        errorTextView.setVisibility(View.GONE);

        // First, check if the invite code is valid
        // In a real app, you might have a separate collection for invite codes
        // For simplicity, we'll use the round ID directly as the invite code
        db.collection("rounds").document(inviteCode)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Round round = document.toObject(Round.class);

                                // Check if user is already a member
                                if (round.getMembers() != null && round.getMembers().contains(currentUser.getUid())) {
                                    showError("You are already a member of this group");
                                } else {
                                    // Add user to the round
                                    addUserToRound(inviteCode, round.getName());
                                }
                            } else {
                                showError("Invalid invite code. Please check and try again.");
                            }
                        } else {
                            showError("Error checking invite code: " + task.getException().getMessage());
                        }
                    }
                });
    }

    private void addUserToRound(String roundId, String roundName) {
        // Add user to the members array
        db.collection("rounds").document(roundId)
                .update("members", FieldValue.arrayUnion(currentUser.getUid()))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Record this activity
                        recordMemberAdded(roundId);

                        Toast.makeText(requireContext(), "Successfully joined the round!", Toast.LENGTH_SHORT).show();

                        // Navigate back to rounds page
                        navigateToRoundsPage();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showError("Error joining round: " + e.getMessage());
                    }
                });
    }

    private void recordMemberAdded(String roundId) {
        if (currentUser == null) return;

        // Get user's name
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            String userName = document.exists() && document.contains("name") ?
                                    document.getString("name") :
                                    (currentUser.getDisplayName() != null ?
                                            currentUser.getDisplayName() : "User");

                            // Create activity record
                            Map<String, Object> activity = new HashMap<>();
                            activity.put("type", "member_added");
                            activity.put("userId", currentUser.getUid());
                            activity.put("userName", userName);
                            activity.put("message", userName + " joined the round");
                            activity.put("timestamp", new Date());

                            // Add to Firestore
                            db.collection("rounds")
                                    .document(roundId)
                                    .collection("activities")
                                    .add(activity);
                        }
                    }
                });
    }

    private void showError(String errorMessage) {
        errorTextView.setText(errorMessage);
        errorTextView.setVisibility(View.VISIBLE);
    }

    private void navigateToRoundsPage() {
        Fragment roundsFragment = new roundsPage();
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, roundsFragment)
                .addToBackStack(null)
                .commit();
    }
}