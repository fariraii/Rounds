package com.example.rounds_fyp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class roundsPage extends Fragment {

    // UI components
    private Button newRoundButton;
    private Button joinExistingRoundButton;
    private ListView roundsListView;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;

    // Data
    private List<Round> roundsList;
    private RoundAdapter roundAdapter;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rounds_page, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI components
        newRoundButton = view.findViewById(R.id.newRoundButton);
        joinExistingRoundButton = view.findViewById(R.id.joinExistingRoundButton);
        roundsListView = view.findViewById(R.id.roundsListView);

        // Add a progress bar and empty state view to your layout and initialize them here
        // progressBar = view.findViewById(R.id.progressBar);
        // emptyStateTextView = view.findViewById(R.id.emptyStateTextView);

        // Initialize rounds list and adapter
        roundsList = new ArrayList<>();
        roundAdapter = new RoundAdapter(requireContext(), roundsList);
        roundsListView.setAdapter(roundAdapter);

        // Set up click listeners
        setupClickListeners();

        // Load rounds from Firebase
        loadRounds();

        return view;
    }

    private void setupClickListeners() {
        // New Round button click
        newRoundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment newRoundFragment = new newRound();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, newRoundFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Join Existing Round button click
        joinExistingRoundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment joinGroupFragment = new joinGroup();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, joinGroupFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Round item click
        roundsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Round selectedRound = roundsList.get(position);
                navigateToRoundDetails(selectedRound);
            }
        });
    }

    private void loadRounds() {
        // Show progress indicator
        // progressBar.setVisibility(View.VISIBLE);

        if (currentUser == null) {
            // User not authenticated, handle this case
            // emptyStateTextView.setText("Please sign in to view your rounds");
            // emptyStateTextView.setVisibility(View.VISIBLE);
            // progressBar.setVisibility(View.GONE);
            return;
        }

        // Query rounds where current user is a member
        db.collection("rounds")
                .whereArrayContains("members", currentUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        // progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            roundsList.clear();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Convert Firestore document to Round object
                                Round round = document.toObject(Round.class);
                                roundsList.add(round);
                            }

                            // Update the adapter
                            roundAdapter.notifyDataSetChanged();

                            // Handle empty state
                            if (roundsList.isEmpty()) {
                                // emptyStateTextView.setText("You haven't joined any rounds yet.\nClick the '+ New Round' button to create one.");
                                // emptyStateTextView.setVisibility(View.VISIBLE);
                            } else {
                                // emptyStateTextView.setVisibility(View.GONE);
                            }
                        } else {
                            Toast.makeText(requireContext(), "Error loading rounds: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void navigateToRoundDetails(Round round) {
        // Bundle to pass the selected round to the details fragment
        Bundle bundle = new Bundle();
        bundle.putSerializable("selected_round", round);

        // Navigate to the round activity fragment
        Fragment roundActivityFragment = new roundActivity();
        roundActivityFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, roundActivityFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the rounds list when returning to this fragment
        loadRounds();
    }
}