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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class newRound extends Fragment {
    // UI components
    private Button proceedButton;
    private Button skipAddMembersButton;
    private TextInputEditText roundNameInput;
    private TextInputEditText contributionAmountInput;
    private Spinner payoutOrderTypeSpinner;
    private TextInputEditText startDateInput;
    private TextInputEditText endDateInput;

    // Data
    private String[] payoutOrderOptions = {"Random", "Fixed (in order of joining)", "Custom"};
    private String selectedPayoutOrderType = "random"; // Default
    private boolean skipAddingMembers = false; // Flag to determine navigation flow

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_round, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI components
        proceedButton = view.findViewById(R.id.proceedButton);
        skipAddMembersButton = view.findViewById(R.id.skipAddMembersButton);
        roundNameInput = view.findViewById(R.id.roundNameInput);
        contributionAmountInput = view.findViewById(R.id.contributionAmountInput);
        payoutOrderTypeSpinner = view.findViewById(R.id.payoutOrderTypeSpinner);
        startDateInput = view.findViewById(R.id.startDateInput);
        endDateInput = view.findViewById(R.id.endDateInput);

        // Set up spinner
        setupPayoutOrderSpinner();

        // Set up click listeners with separate handling
        proceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                skipAddingMembers = false; // Make sure we navigate to add members
                createRound();
            }
        });

        skipAddMembersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                skipAddingMembers = true; // Set flag to skip adding members
                createRound();
            }
        });

        return view;
    }

    private void setupPayoutOrderSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                payoutOrderOptions);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        payoutOrderTypeSpinner.setAdapter(adapter);

        payoutOrderTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        selectedPayoutOrderType = "random";
                        break;
                    case 1:
                        selectedPayoutOrderType = "fixed";
                        break;
                    case 2:
                        selectedPayoutOrderType = "custom";
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Default to random
                selectedPayoutOrderType = "random";
            }
        });
    }

    private void createRound() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You must be logged in to create a round", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get input values
        String roundName = roundNameInput.getText().toString().trim();
        String contributionAmountStr = contributionAmountInput.getText().toString().trim();
        String contributionDateStr = startDateInput.getText().toString().trim();
        String durationMonthsStr = endDateInput.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(roundName)) {
            roundNameInput.setError("Round name is required");
            return;
        }

        if (TextUtils.isEmpty(contributionAmountStr)) {
            contributionAmountInput.setError("Contribution amount is required");
            return;
        }

        if (TextUtils.isEmpty(contributionDateStr)) {
            startDateInput.setError("Contribution date is required");
            return;
        }

        if (TextUtils.isEmpty(durationMonthsStr)) {
            endDateInput.setError("Duration is required");
            return;
        }

        // Parse values
        double contributionAmount;
        int contributionDate;
        int durationMonths;

        try {
            contributionAmount = Double.parseDouble(contributionAmountStr);
            contributionDate = Integer.parseInt(contributionDateStr);
            durationMonths = Integer.parseInt(durationMonthsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate ranges
        if (contributionAmount <= 0) {
            contributionAmountInput.setError("Amount must be greater than 0");
            return;
        }

        if (contributionDate < 1 || contributionDate > 30) {
            startDateInput.setError("Date must be between 1 and 30");
            return;
        }

        if (durationMonths < 1 || durationMonths > 12) {
            endDateInput.setError("Duration must be between 1 and 12 months");
            return;
        }

        // Create Round document in Firestore
        Map<String, Object> roundData = new HashMap<>();
        roundData.put("name", roundName);
        roundData.put("contributionAmount", contributionAmount);
        roundData.put("contributionDate", contributionDate);
        roundData.put("payoutOrderType", selectedPayoutOrderType);
        roundData.put("durationMonths", durationMonths);
        roundData.put("creatorId", currentUser.getUid());
        roundData.put("createdAt", new Date());

        // Initialize with creator as first member
        List<String> members = new ArrayList<>();
        members.add(currentUser.getUid());
        roundData.put("members", members);

        roundData.put("isActive", true);

        // Disable buttons to prevent double submission
        proceedButton.setEnabled(false);
        skipAddMembersButton.setEnabled(false);

        // Save to Firestore
        db.collection("rounds")
                .add(roundData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Record round creation activity
                        String roundId = documentReference.getId();
                        recordRoundCreation(roundId);

                        // Update the document with its ID
                        db.collection("rounds").document(roundId)
                                .update("id", roundId)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(requireContext(), "Round created successfully", Toast.LENGTH_SHORT).show();

                                        // Create Round object to pass to the next screen
                                        Round newRound = new Round(
                                                roundId,
                                                roundName,
                                                contributionAmount,
                                                contributionDate,
                                                selectedPayoutOrderType,
                                                durationMonths,
                                                currentUser.getUid()
                                        );
                                        newRound.addMember(currentUser.getUid());

                                        // Now navigate based on the flag
                                        if (skipAddingMembers) {
                                            // Skip to rounds page
                                            navigateToRoundsPage();
                                        } else {
                                            // Go to add users screen
                                            navigateToAddUsers(newRound);
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireContext(), "Error creating round: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();

                        // Re-enable buttons if there's an error
                        proceedButton.setEnabled(true);
                        skipAddMembersButton.setEnabled(true);
                    }
                });
    }

    private void recordRoundCreation(String roundId) {
        if (currentUser == null) return;

        // Create activity record
        Map<String, Object> activity = new HashMap<>();
        activity.put("type", "round_created");
        activity.put("userId", currentUser.getUid());
        activity.put("userName", currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "User");
        activity.put("message", "Round was created");
        activity.put("timestamp", new Date());

        // Add to Firestore
        db.collection("rounds")
                .document(roundId)
                .collection("activities")
                .add(activity);
    }

    private void navigateToAddUsers(Round round) {
        Fragment addUsersFragment = new addUsers();

        // Pass the round data
        Bundle args = new Bundle();
        args.putSerializable("selected_round", round);
        addUsersFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, addUsersFragment)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToRoundsPage() {
        Fragment roundsPageFragment = new roundsPage();

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, roundsPageFragment)
                .addToBackStack(null)
                .commit();
    }
}