package com.example.rounds_fyp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class roundActivity extends Fragment {

    // UI components
    private TextView groupNameTextView;
    private Button makeContributionButton;
    private TabLayout tabLayout;
    private RecyclerView recyclerViewActivity;

    // Data
    private Round currentRound;
    private List<ActivityItem> activityItems;
    private ActivityAdapter activityAdapter;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private com.google.firebase.auth.FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_round_activity, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI components
        groupNameTextView = view.findViewById(R.id.groupName);
        makeContributionButton = view.findViewById(R.id.btnMakeContribution);
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerViewActivity = view.findViewById(R.id.recyclerViewActivity);

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

        // Set up recycler view
        setupRecyclerView();

        // Load activity data
        loadActivityData();

        // Set up click listeners
        setupClickListeners();

        // Set up tab selection listener
        setupTabListener();
        tabLayout.getTabAt(0).select();

        return view;
    }

    private void displayRoundDetails() {
        if (currentRound != null) {
            groupNameTextView.setText(currentRound.getName());
        }
    }

    private void setupRecyclerView() {
        activityItems = new ArrayList<>();
        activityAdapter = new ActivityAdapter(activityItems);
        recyclerViewActivity.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewActivity.setAdapter(activityAdapter);
    }

    private void loadActivityData() {
        if (currentRound == null) return;

        // Query Firestore for activities related to this round
        db.collection("rounds")
                .document(currentRound.getId())
                .collection("activities")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Most recent first
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            activityItems.clear();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    // Convert document to ActivityItem
                                    ActivityItem item = document.toObject(ActivityItem.class);

                                    // Log data to debug
                                    System.out.println("Activity data: " + document.getData());

                                    // Ensure we have values for critical fields
                                    if (item.getType() == null) {
                                        item.setType("unknown");
                                    }

                                    if (item.getUserName() == null || item.getUserName().trim().isEmpty()) {
                                        // Try to get the user name from the document data directly
                                        Object userName = document.get("userName");
                                        if (userName != null && userName instanceof String) {
                                            item.setUserName((String) userName);
                                        } else {
                                            item.setUserName("Unknown User");
                                        }
                                    }

                                    activityItems.add(item);
                                } catch (Exception e) {
                                    System.err.println("Error processing activity: " + e.getMessage());
                                }
                            }

                            // If no activities, add a placeholder message
                            if (activityItems.isEmpty()) {
                                ActivityItem placeholder = new ActivityItem();
                                placeholder.setType("placeholder");
                                placeholder.setMessage("No activity yet for this round.");
                                placeholder.setTimestamp(new Date());
                                activityItems.add(placeholder);
                            }

                            activityAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(requireContext(), "Error loading activities: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupClickListeners() {
        makeContributionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToContribution();
            }
        });
    }

    private void setupTabListener() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // Activity tab
                        // Already showing activity, no action needed
                        break;
                    case 1: // Manage tab
                        navigateToManageRound();
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


    private void navigateToContribution() {
        Fragment addContributionFragment = new addContribution();

        // Pass the round data to the contribution fragment
        Bundle args = new Bundle();
        args.putSerializable("selected_round", currentRound);
        addContributionFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, addContributionFragment)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToManageRound() {
        Fragment manageRoundFragment = new manageRound();

        // Pass the round data to the manage fragment
        Bundle args = new Bundle();
        args.putSerializable("selected_round", currentRound);
        manageRoundFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, manageRoundFragment)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToRoundMembers() {
        Fragment roundMembersFragment = new roundMembers();

        // Pass the round data to the members fragment
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

    // Inner class for Activity items
    public static class ActivityItem {
        private String type;
        private String userId;
        private String userName;
        private String message;
        private Date timestamp;
        private double amount;

        public ActivityItem() {
            // Required empty constructor for Firestore
            // Initialize with default values to prevent null pointer exceptions
            this.type = "unknown";
            this.userId = "";
            this.userName = "Unknown User";
            this.message = "";
            this.timestamp = new Date();
            this.amount = 0.0;
        }

        // Getters and setters
        public String getType() {
            return type != null ? type : "unknown";
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUserId() {
            return userId != null ? userId : "";
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName != null ? userName : "Unknown User";
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getMessage() {
            return message != null ? message : "";
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Date getTimestamp() {
            return timestamp != null ? timestamp : new Date();
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

        public String getFormattedDate() {
            if (timestamp == null) return "";
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(timestamp);
        }

        @Override
        public String toString() {
            return "ActivityItem{" +
                    "type='" + type + '\'' +
                    ", userId='" + userId + '\'' +
                    ", userName='" + userName + '\'' +
                    ", message='" + message + '\'' +
                    ", timestamp=" + timestamp +
                    ", amount=" + amount +
                    '}';
        }
    }

    // Adapter for the activity recycler view
    private class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {
        private List<ActivityItem> items;

        public ActivityAdapter(List<ActivityItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_activity, parent, false);
            return new ActivityViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
            ActivityItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ActivityViewHolder extends RecyclerView.ViewHolder {
            TextView activityTextView;
            TextView dateTextView;

            public ActivityViewHolder(@NonNull View itemView) {
                super(itemView);
                activityTextView = itemView.findViewById(R.id.activityTextView);
                dateTextView = itemView.findViewById(R.id.dateTextView);
            }

            public void bind(ActivityItem item) {
                // Set the activity text based on type
                if ("contribution".equals(item.getType())) {
                    String displayName = getDisplayName(item.getUserId(), item.getUserName());
                    activityTextView.setText(displayName + " contributed £" +
                            String.format(Locale.getDefault(), "%.2f", item.getAmount()));
                } else if ("payout".equals(item.getType())) {
                    String displayName = getDisplayName(item.getUserId(), item.getUserName());
                    activityTextView.setText(displayName + " received a payout of £" +
                            String.format(Locale.getDefault(), "%.2f", item.getAmount()));
                } else if ("member_added".equals(item.getType())) {
                    String displayName = getDisplayName(item.getUserId(), item.getUserName());
                    activityTextView.setText(displayName + " joined the round");
                } else if ("member_removed".equals(item.getType())) {
                    String displayName = getDisplayName(item.getUserId(), item.getUserName());
                    activityTextView.setText(displayName + " left the round");
                } else if ("round_created".equals(item.getType())) {
                    String displayName = getDisplayName(item.getUserId(), item.getUserName());
                    activityTextView.setText("Round was created by " + displayName);
                } else if ("round_updated".equals(item.getType())) {
                    String displayName = getDisplayName(item.getUserId(), item.getUserName());
                    activityTextView.setText("Round details were updated by " + displayName);
                } else if ("placeholder".equals(item.getType())) {
                    activityTextView.setText(item.getMessage());
                } else {
                    activityTextView.setText(item.getMessage());
                }

                // Set the date
                dateTextView.setText(item.getFormattedDate());
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
        }
    }
}