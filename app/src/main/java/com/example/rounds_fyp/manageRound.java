package com.example.rounds_fyp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class manageRound extends Fragment {

    // UI components
    private TextView groupTitleTextView;
    private EditText editRoundNameEditText;
    private Spinner spinnerPaymentOrder;
    private TextView contributionDateTextView;
    private Button btnSaveRound;
    private TabLayout tabLayout;

    // Data
    private Round currentRound;
    private String[] paymentOrderOptions = {"Random", "Fixed (in order of joining)", "Custom"};

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_round, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI components
        groupTitleTextView = view.findViewById(R.id.groupTitle);
        editRoundNameEditText = view.findViewById(R.id.editRoundName);
        spinnerPaymentOrder = view.findViewById(R.id.spinnerPaymentOrder);
        contributionDateTextView = view.findViewById(R.id.textViewContributionDate);
        btnSaveRound = view.findViewById(R.id.btnSaveRound);
        tabLayout = view.findViewById(R.id.tabLayout);

        // Get the round from arguments
        Bundle args = getArguments();
        if (args != null && args.containsKey("selected_round")) {
            currentRound = (Round) args.getSerializable("selected_round");
            displayRoundDetails();
        } else {
            // Handle case where round data is missing
            Toast.makeText(requireContext(), "Error: Round details not available", Toast.LENGTH_SHORT).show();
            goBack();
        }

        // Set up the payment order spinner
        setupPaymentOrderSpinner();

        // Set up click listeners
        setupClickListeners();

        // Set up tab selection listener
        setupTabListener();
        tabLayout.getTabAt(1).select();

        return view;
    }

    private void displayRoundDetails() {
        if (currentRound != null) {
            groupTitleTextView.setText(currentRound.getName());
            editRoundNameEditText.setText(currentRound.getName());

            // Set the contribution date text
            contributionDateTextView.setText("Contribution Date: " +
                    currentRound.getContributionDate() + "th Monthly");

            // Set the selected payment order option in the spinner
            int position = 0; // Default to "Random"
            String payoutOrderType = currentRound.getPayoutOrderType();

            if ("fixed".equals(payoutOrderType)) {
                position = 1;
            } else if ("custom".equals(payoutOrderType)) {
                position = 2;
            }

            spinnerPaymentOrder.setSelection(position);
        }
    }

    private void setupPaymentOrderSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                paymentOrderOptions);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentOrder.setAdapter(adapter);

        spinnerPaymentOrder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Handle payment order selection
                if (position == 2) { // Custom
                    // If "Custom" is selected, we should navigate to members tab to arrange order
                    Toast.makeText(requireContext(),
                            "Please go to the Members tab to arrange custom payment order",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupClickListeners() {
        btnSaveRound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRoundChanges();
            }
        });

        // Add a long click listener on the group title to share the invite code
        groupTitleTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                shareInviteCode();
                return true;
            }
        });
    }

    private void shareInviteCode() {
        if (currentRound == null) return;

        // The invite code is simply the round ID
        String inviteCode = currentRound.getId();

        // Copy to clipboard
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Rounds Invite Code", inviteCode);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(requireContext(),
                "Invite code copied to clipboard!\nShare this with others to let them join.",
                Toast.LENGTH_LONG).show();
    }

    private void setupTabListener() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // Activity tab
                        navigateToRoundActivity();
                        break;
                    case 1: // Manage tab
                        // Already on manage tab, no action needed
                        break;
                    case 2: // Members tab
                        navigateToRoundMembers();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }

    private void saveRoundChanges() {
        if (currentRound == null || currentUser == null) return;

        // Get values from UI
        String roundName = editRoundNameEditText.getText().toString().trim();
        String payoutOrderType = getPayoutOrderTypeFromSpinner();

        // Validate input
        if (roundName.isEmpty()) {
            Toast.makeText(requireContext(), "Round name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update round object
        currentRound.setName(roundName);
        currentRound.setPayoutOrderType(payoutOrderType);

        // Save to Firestore
        db.collection("rounds").document(currentRound.getId())
                .set(currentRound)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Record this activity
                        recordRoundUpdate();

                        Toast.makeText(requireContext(), "Round updated successfully", Toast.LENGTH_SHORT).show();
                        groupTitleTextView.setText(roundName);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireContext(), "Error updating round: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getPayoutOrderTypeFromSpinner() {
        int position = spinnerPaymentOrder.getSelectedItemPosition();
        switch (position) {
            case 0:
                return "random";
            case 1:
                return "fixed";
            case 2:
                return "custom";
            default:
                return "random";
        }
    }

    private void recordRoundUpdate() {
        if (currentRound == null || currentUser == null) return;

        // Create activity record
        Map<String, Object> activity = new HashMap<>();
        activity.put("type", "round_updated");
        activity.put("userId", currentUser.getUid());
        activity.put("userName", currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "User");
        activity.put("message", "Round details were updated");
        activity.put("timestamp", new Date());

        // Add to Firestore
        db.collection("rounds")
                .document(currentRound.getId())
                .collection("activities")
                .add(activity)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        // Activity recorded, no need for user feedback
                    }
                });
    }

    private void navigateToRoundActivity() {
        Fragment roundActivityFragment = new roundActivity();

        // Pass the round data
        Bundle args = new Bundle();
        args.putSerializable("selected_round", currentRound);
        roundActivityFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, roundActivityFragment)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToRoundMembers() {
        Fragment roundMembersFragment = new roundMembers();

        // Pass the round data
        Bundle args = new Bundle();
        args.putSerializable("selected_round", currentRound);
        roundMembersFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, roundMembersFragment)
                .addToBackStack(null)
                .commit();
    }

    private void goBack() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}