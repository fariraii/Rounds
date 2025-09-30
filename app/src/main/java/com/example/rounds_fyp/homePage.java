package com.example.rounds_fyp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class homePage extends Fragment implements withdraw.BalanceUpdateListener {

    Button withdrawButton;
    Button activityInsightsButton;
    Button addContributionButton;
    ImageView notificationIcon;
    TextView balanceTextView;
    RecyclerView activeRoundsList;

    // Adapter for active rounds
    private ActiveRoundsAdapter activeRoundsAdapter;
    private List<Round> activeRounds;

    // Constants for SharedPreferences
    private static final String PREF_NAME = "RoundsPreferences";
    private static final String BALANCE_KEY = "currentBalance";

    // Default balance if no balance is stored yet
    private static final double DEFAULT_BALANCE = 267.0;

    private double currentBalance;

    // Tag for logging
    private static final String TAG = "HomePage";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home_page, container, false);
        withdrawButton = view.findViewById(R.id.withdrawButton);
        activityInsightsButton = view.findViewById(R.id.activityInsightsButton);
        addContributionButton = view.findViewById(R.id.addContributionButton);
        notificationIcon = view.findViewById(R.id.notificationIcon);

        // Set up RecyclerView for active rounds
        activeRoundsList = view.findViewById(R.id.activeRoundsList);
        setupActiveRoundsList();

        // Find the balance TextView (within the balanceCard)
        balanceTextView = view.findViewById(R.id.balanceAmount);

        // Load and display the current balance
        loadBalance();

        withdrawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment withdrawFragment = new withdraw();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, withdrawFragment)
                        .addToBackStack(null)
                        .commit();

                // Register this fragment as a balance update listener
                ((MainActivity) requireActivity()).setBalanceUpdateListener(homePage.this);
            }
        });

        activityInsightsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment activityInsightsFragment = new activityInsights();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, activityInsightsFragment)
                        .addToBackStack(null)
                        .commit();
            }

        });

        addContributionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment addContributionFragment = new addContribution();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, addContributionFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        notificationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment notificationsFragment = new notifications_page();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, notificationsFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        TextView welcomeText = view.findViewById(R.id.welcomeText);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String fullName = document.getString("fullName");

                            if (fullName != null && !fullName.isEmpty()) {
                                welcomeText.setText("Hello, " + fullName + "!");
                            } else {
                                welcomeText.setText("Welcome back!");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error fetching home page user data", e);
                        welcomeText.setText("Welcome!");
                    });
        }

        // Set the current date
        TextView todaysDate = view.findViewById(R.id.todaysDate);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        todaysDate.setText(sdf.format(new Date()));

        return view;
    }

    private void setupActiveRoundsList() {
        activeRounds = new ArrayList<>();
        activeRoundsAdapter = new ActiveRoundsAdapter(requireContext(), activeRounds);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        activeRoundsList.setLayoutManager(layoutManager);
        activeRoundsList.setAdapter(activeRoundsAdapter);

        // Load active rounds from Firestore
        loadActiveRounds();
    }

    private void loadActiveRounds() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user logged in");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Query rounds where current user is a member and the round is active
        db.collection("rounds")
                .whereArrayContains("members", currentUser.getUid())
                .whereEqualTo("isActive", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        activeRounds.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Round round = document.toObject(Round.class);
                            activeRounds.add(round);
                            Log.d(TAG, "Round loaded: " + round.getName());
                        }

                        // Update the adapter
                        activeRoundsAdapter.notifyDataSetChanged();

                        // Show/hide empty state if needed
                        if (activeRounds.isEmpty()) {
                            // Here you could show an empty state view
                            Log.d(TAG, "No active rounds found");
                        }
                    } else {
                        Log.e(TAG, "Error loading rounds", task.getException());
                        Toast.makeText(requireContext(), "Error loading rounds", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload balance when returning to this fragment
        loadBalance();

        // Reload active rounds
        loadActiveRounds();
    }

    private void loadBalance() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentBalance = prefs.getFloat(BALANCE_KEY, (float) DEFAULT_BALANCE);
        updateBalanceDisplay();
    }

    private void updateBalanceDisplay() {
        // Format balance with £ sign and 2 decimal places
        if (balanceTextView != null) {
            balanceTextView.setText(String.format("£%.2f", currentBalance));
        }
    }

    @Override
    public void onBalanceUpdated(double newBalance) {
        // Update the balance when notified of a change
        currentBalance = newBalance;
        updateBalanceDisplay();
    }
}