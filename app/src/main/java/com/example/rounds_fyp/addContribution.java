package com.example.rounds_fyp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class addContribution extends Fragment {

    // UI components
    private Spinner groupSpinner;
    private TextInputEditText amountInput;
    private Button proceedToPaymentButton;

    // Data
    private List<Round> userRounds;
    private Round selectedRound;
    private ArrayAdapter<String> roundsAdapter;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_contribution, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI components
        groupSpinner = view.findViewById(R.id.groupSpinner);
        amountInput = view.findViewById(R.id.amountInput);
        proceedToPaymentButton = view.findViewById(R.id.proceedToPaymentButton);

        // Initialize data
        userRounds = new ArrayList<>();

        // Check if a round was passed from arguments
        Bundle args = getArguments();
        if (args != null && args.containsKey("selected_round")) {
            selectedRound = (Round) args.getSerializable("selected_round");
            // If a round is passed, we'll pre-select it in the spinner
        }

        // Set up spinner
        setupRoundsSpinner();

        // Load user's rounds
        loadUserRounds();

        // Set up click listeners
        proceedToPaymentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processContribution();
            }
        });

        return view;
    }

    private void setupRoundsSpinner() {
        // Initialize adapter with empty list (will be populated later)
        List<String> roundNames = new ArrayList<>();
        roundsAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, roundNames);
        roundsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(roundsAdapter);

        // Set selection listener
        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < userRounds.size()) {
                    selectedRound = userRounds.get(position);

                    // Pre-fill the amount field with the round's contribution amount
                    if (selectedRound != null) {
                        amountInput.setText(String.valueOf(selectedRound.getContributionAmount()));
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedRound = null;
            }
        });
    }

    private void loadUserRounds() {
        if (currentUser == null) return;

        // Query rounds where current user is a member
        db.collection("rounds")
                .whereArrayContains("members", currentUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            userRounds.clear();
                            List<String> roundNames = new ArrayList<>();

                            int preSelectedPosition = -1;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Round round = document.toObject(Round.class);
                                userRounds.add(round);
                                roundNames.add(round.getName());

                                // If this is the pre-selected round, note its position
                                if (selectedRound != null && round.getId().equals(selectedRound.getId())) {
                                    preSelectedPosition = userRounds.size() - 1;
                                }
                            }

                            // Update adapter and spinner
                            roundsAdapter.clear();
                            roundsAdapter.addAll(roundNames);
                            roundsAdapter.notifyDataSetChanged();

                            // Select the pre-selected round if available
                            if (preSelectedPosition >= 0) {
                                groupSpinner.setSelection(preSelectedPosition);
                            }
                        } else {
                            Toast.makeText(requireContext(), "Error loading rounds: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void processContribution() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You must be logged in to make a contribution",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedRound == null) {
            Toast.makeText(requireContext(), "Please select a round", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = amountInput.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            amountInput.setError("Please enter an amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            amountInput.setError("Please enter a valid amount");
            return;
        }

        if (amount <= 0) {
            amountInput.setError("Amount must be greater than 0");
            return;
        }

        // In a real app, you would process payment here
        // For demonstration, we'll simulate a successful payment

        // Record the contribution
        recordContribution(amount);
    }

    private void recordContribution(double amount) {
        // Create the contribution record
        Map<String, Object> contribution = new HashMap<>();
        contribution.put("userId", currentUser.getUid());
        contribution.put("roundId", selectedRound.getId());
        contribution.put("amount", amount);
        contribution.put("timestamp", new Date());
        contribution.put("status", "completed");

        // Save to Firestore
        db.collection("contributions")
                .add(contribution)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Record this activity in the round
                        recordContributionActivity(amount);

                        Toast.makeText(requireContext(), "Contribution successful!", Toast.LENGTH_SHORT).show();

                        // Navigate back to round activity
                        navigateToRoundActivity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireContext(), "Error recording contribution: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void recordContributionActivity(double amount) {
        if (currentUser == null) return;

        // Get user's name
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            String userName = "Unknown User";

                            // Try to get name from different possible fields
                            if (document.exists()) {
                                if (document.contains("name")) {
                                    userName = document.getString("name");
                                } else if (document.contains("displayName")) {
                                    userName = document.getString("displayName");
                                } else if (currentUser.getDisplayName() != null) {
                                    userName = currentUser.getDisplayName();
                                } else if (document.contains("email")) {
                                    // Use email as fallback
                                    String email = document.getString("email");
                                    if (email != null && email.contains("@")) {
                                        userName = email.split("@")[0]; // Use part before @
                                    }
                                }
                            } else if (currentUser.getDisplayName() != null) {
                                userName = currentUser.getDisplayName();
                            } else if (currentUser.getEmail() != null) {
                                String email = currentUser.getEmail();
                                if (email.contains("@")) {
                                    userName = email.split("@")[0]; // Use part before @
                                }
                            }

                            // Make sure we don't have empty userName
                            if (userName == null || userName.trim().isEmpty()) {
                                userName = "User";
                            }

                            // Create activity record
                            Map<String, Object> activity = new HashMap<>();
                            activity.put("type", "contribution");
                            activity.put("userId", currentUser.getUid());
                            activity.put("userName", userName);
                            activity.put("message", userName + " contributed £" + String.format("%.2f", amount));
                            activity.put("amount", amount);
                            activity.put("timestamp", new Date());

                            // Add to Firestore
                            String finalUserName = userName;
                            db.collection("rounds")
                                    .document(selectedRound.getId())
                                    .collection("activities")
                                    .add(activity)
                                    .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentReference> task) {
                                            if (task.isSuccessful()) {
                                                System.out.println("Activity recorded successfully with userName: " + finalUserName);
                                            } else {
                                                System.err.println("Failed to record activity: " + task.getException());
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void navigateToRoundActivity() {
        Fragment roundActivityFragment = new roundActivity();

        // Pass the round data
        Bundle args = new Bundle();
        args.putSerializable("selected_round", selectedRound);
        roundActivityFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, roundActivityFragment)
                .addToBackStack(null)
                .commit();
    }
}