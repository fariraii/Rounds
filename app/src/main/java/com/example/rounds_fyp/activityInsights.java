package com.example.rounds_fyp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class activityInsights extends Fragment {

    // UI components
    private ListView activityListView;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;

    // Data
    private List<GlobalActivityItem> activityItems;
    private GlobalActivityAdapter activityAdapter;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_activity_insights, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI components
        activityListView = view.findViewById(R.id.activityListView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateTextView = view.findViewById(R.id.emptyStateText);

        // Hide empty state initially
        if (emptyStateTextView != null) {
            emptyStateTextView.setVisibility(View.GONE);
        }

        // Initialize data structures
        activityItems = new ArrayList<>();
        activityAdapter = new GlobalActivityAdapter(requireContext(), activityItems);
        activityListView.setAdapter(activityAdapter);

        // Load activities
        loadActivities();

        return view;
    }

    private void loadActivities() {
        // Show progress indicator
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (currentUser == null) {
            // User not authenticated, handle this case
            if (emptyStateTextView != null) {
                emptyStateTextView.setText("Please sign in to view your activities");
                emptyStateTextView.setVisibility(View.VISIBLE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            return;
        }

        // First, get all rounds that the current user is a member of
        db.collection("rounds")
                .whereArrayContains("members", currentUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                // No rounds found
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                                if (emptyStateTextView != null) {
                                    emptyStateTextView.setText("No rounds found. Join or create a round to see activity.");
                                    emptyStateTextView.setVisibility(View.VISIBLE);
                                }
                                return;
                            }

                            // Process each round
                            List<String> roundIds = new ArrayList<>();
                            List<String> roundNames = new ArrayList<>();

                            for (QueryDocumentSnapshot roundDoc : task.getResult()) {
                                String roundId = roundDoc.getId();
                                String roundName = roundDoc.getString("name");
                                roundIds.add(roundId);
                                roundNames.add(roundName);
                            }

                            // Now that we have all round IDs, get activities for each round
                            fetchActivitiesForRounds(roundIds, roundNames);
                        } else {
                            // Error getting rounds
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            Toast.makeText(requireContext(), "Error loading rounds: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void fetchActivitiesForRounds(List<String> roundIds, List<String> roundNames) {
        final int[] completedRequests = {0};
        final int totalRounds = roundIds.size();
        activityItems.clear();

        for (int i = 0; i < totalRounds; i++) {
            final String roundId = roundIds.get(i);
            final String roundName = roundNames.get(i);

            db.collection("rounds")
                    .document(roundId)
                    .collection("activities")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50) // Limit to recent activities per round
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    try {
                                        // Get data from document
                                        String type = document.getString("type");
                                        String userId = document.getString("userId");
                                        String userName = document.getString("userName");
                                        String message = document.getString("message");
                                        Date timestamp = document.getDate("timestamp");
                                        Double amount = document.getDouble("amount");

                                        // Create activity item
                                        GlobalActivityItem item = new GlobalActivityItem();
                                        item.setType(type != null ? type : "unknown");
                                        item.setUserId(userId);
                                        item.setUserName(userName != null ? userName : "Unknown User");
                                        item.setMessage(message != null ? message : "");
                                        item.setTimestamp(timestamp != null ? timestamp : new Date());
                                        item.setAmount(amount != null ? amount : 0.0);
                                        item.setRoundName(roundName);
                                        item.setRoundId(roundId);

                                        activityItems.add(item);
                                    } catch (Exception e) {
                                        System.err.println("Error processing activity: " + e.getMessage());
                                    }
                                }
                            } else {
                                System.err.println("Error getting activities for round " + roundId + ": " + task.getException().getMessage());
                            }

                            // Increment completed requests counter
                            completedRequests[0]++;

                            // If all requests are complete, update UI
                            if (completedRequests[0] >= totalRounds) {
                                updateActivityListUI();
                            }
                        }
                    });
        }
    }

    private void updateActivityListUI() {
        // Sort all activities by timestamp (newest first)
        Collections.sort(activityItems, new Comparator<GlobalActivityItem>() {
            @Override
            public int compare(GlobalActivityItem o1, GlobalActivityItem o2) {
                return o2.getTimestamp().compareTo(o1.getTimestamp());
            }
        });

        // Update UI
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (activityItems.isEmpty()) {
            // No activities found
            if (emptyStateTextView != null) {
                emptyStateTextView.setText("No activities found in your rounds yet.");
                emptyStateTextView.setVisibility(View.VISIBLE);
            }
        } else {
            // Activities found, update adapter
            if (emptyStateTextView != null) {
                emptyStateTextView.setVisibility(View.GONE);
            }
            activityAdapter.notifyDataSetChanged();
        }
    }

    // Model class for global activity items
    public static class GlobalActivityItem {
        private String type;
        private String userId;
        private String userName;
        private String message;
        private Date timestamp;
        private double amount;
        private String roundName;
        private String roundId;

        public GlobalActivityItem() {
            // Required empty constructor
            this.type = "unknown";
            this.userId = "";
            this.userName = "Unknown User";
            this.message = "";
            this.timestamp = new Date();
            this.amount = 0.0;
            this.roundName = "";
            this.roundId = "";
        }

        // Getters and setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getRoundName() {
            return roundName;
        }

        public void setRoundName(String roundName) {
            this.roundName = roundName;
        }

        public String getRoundId() {
            return roundId;
        }

        public void setRoundId(String roundId) {
            this.roundId = roundId;
        }

        public String getFormattedDate() {
            if (timestamp == null) return "";
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return sdf.format(timestamp);
        }
    }

    // Adapter for the global activity list
    private class GlobalActivityAdapter extends android.widget.ArrayAdapter<GlobalActivityItem> {
        private final Context context;
        private final List<GlobalActivityItem> items;

        public GlobalActivityAdapter(Context context, List<GlobalActivityItem> items) {
            super(context, R.layout.item_global_activity, items);
            this.context = context;
            this.items = items;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.item_global_activity, parent, false);
            }

            GlobalActivityItem item = items.get(position);

            TextView activityTextView = convertView.findViewById(R.id.activityTextView);
            TextView roundNameTextView = convertView.findViewById(R.id.roundNameTextView);
            TextView dateTextView = convertView.findViewById(R.id.dateTextView);
            ImageView activityIconImageView = convertView.findViewById(R.id.activityIconImageView);

            // Set activity text based on type
            String activityText = getActivityText(item);
            activityTextView.setText(activityText);

            // Set round name
            roundNameTextView.setText(item.getRoundName());

            // Set date
            dateTextView.setText(item.getFormattedDate());

            // Set icon based on activity type
            setActivityIcon(activityIconImageView, item.getType());

            // Set click listener to navigate to the specific round
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigateToRound(item.getRoundId(), item.getRoundName());
                }
            });

            return convertView;
        }

        private String getActivityText(GlobalActivityItem item) {
            String displayName = getDisplayName(item.getUserId(), item.getUserName());

            switch (item.getType()) {
                case "contribution":
                    return displayName + " contributed £" + String.format(Locale.getDefault(), "%.2f", item.getAmount());
                case "payout":
                    return displayName + " received a payout of £" + String.format(Locale.getDefault(), "%.2f", item.getAmount());
                case "member_added":
                    return displayName + " joined the round";
                case "member_removed":
                    return displayName + " left the round";
                case "round_created":
                    return "Round was created by " + displayName;
                case "round_updated":
                    return "Round details were updated by " + displayName;
                default:
                    return item.getMessage(); // Use message field as fallback
            }
        }

        private String getDisplayName(String userId, String userName) {
            // If the activity was done by the current user, display "You" instead of their name
            if (currentUser != null && userId != null && userId.equals(currentUser.getUid())) {
                return "You";
            }

            // Make sure we don't display null or empty usernames
            if (userName == null || userName.trim().isEmpty()) {
                return "Unknown User";
            }

            return userName;
        }

        private void setActivityIcon(ImageView imageView, String activityType) {
            // Set different icons based on activity type
            switch (activityType) {
                case "contribution":
                    imageView.setImageResource(R.drawable.baseline_attach_money_24);
                    imageView.setColorFilter(context.getResources().getColor(R.color.turquoise));
                    break;
                case "payout":
                    imageView.setImageResource(R.drawable.baseline_account_balance_wallet_24);
                    imageView.setColorFilter(context.getResources().getColor(R.color.dark_purple));
                    break;
                case "member_added":
                    imageView.setImageResource(R.drawable.baseline_person_add_24);
                    imageView.setColorFilter(context.getResources().getColor(R.color.mint_green));
                    break;
                case "member_removed":
                    imageView.setImageResource(R.drawable.baseline_person_remove_24);
                    imageView.setColorFilter(context.getResources().getColor(android.R.color.holo_red_light));
                    break;
                case "round_created":
                    imageView.setImageResource(R.drawable.baseline_add_circle_outline_24);
                    imageView.setColorFilter(context.getResources().getColor(R.color.turquoise));
                    break;
                case "round_updated":
                    imageView.setImageResource(R.drawable.baseline_edit_24);
                    imageView.setColorFilter(context.getResources().getColor(R.color.dark_purple));
                    break;
                default:
                    imageView.setImageResource(R.drawable.baseline_circle_24);
                    imageView.setColorFilter(context.getResources().getColor(R.color.turquoise));
                    break;
            }
        }

        private void navigateToRound(String roundId, String roundName) {
            // Fetch the complete Round object from Firestore
            db.collection("rounds").document(roundId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                // Convert to Round object
                                Round round = task.getResult().toObject(Round.class);

                                if (round != null) {
                                    // Make sure the ID is set (it's not automatically mapped)
                                    round.setId(roundId);

                                    // Navigate to round activity
                                    Fragment roundActivityFragment = new roundActivity();

                                    // Pass the round data
                                    Bundle args = new Bundle();
                                    args.putSerializable("selected_round", round);
                                    roundActivityFragment.setArguments(args);

                                    requireActivity().getSupportFragmentManager().beginTransaction()
                                            .replace(R.id.nav_host_fragment, roundActivityFragment)
                                            .addToBackStack(null)
                                            .commit();
                                } else {
                                    Toast.makeText(context, "Error loading round details", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(context, "Error loading round details", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload activities when returning to this fragment
        loadActivities();
    }
}