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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class roundMembers extends Fragment {

    // UI components
    private TextView groupTitleTextView;
    private RecyclerView recyclerViewMembers;
    private Button btnAddNewUser;
    private Button btnInviteUser;
    private TabLayout tabLayout;

    // Data
    private Round currentRound;
    private List<User> membersList;
    private MemberAdapter memberAdapter;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_round_members, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI components
        groupTitleTextView = view.findViewById(R.id.groupTitle);
        recyclerViewMembers = view.findViewById(R.id.recyclerViewMembers);
        btnAddNewUser = view.findViewById(R.id.btnAddNewUser);
        btnInviteUser = view.findViewById(R.id.btnInviteUser);
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

        // Set up recycler view
        setupRecyclerView();

        // Load members data
        loadMembersData();

        // Set up click listeners
        setupClickListeners();

        // Set up tab selection listener
        setupTabListener();
        tabLayout.getTabAt(2).select();

        return view;
    }

    private void displayRoundDetails() {
        if (currentRound != null) {
            groupTitleTextView.setText(currentRound.getName());
        }
    }

    private void setupRecyclerView() {
        membersList = new ArrayList<>();
        memberAdapter = new MemberAdapter(membersList);

        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewMembers.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        recyclerViewMembers.setAdapter(memberAdapter);

        // Add drag-and-drop functionality for custom payment order
        if ("custom".equals(currentRound.getPayoutOrderType())) {
            ItemTouchHelper.Callback callback = new MemberItemTouchHelperCallback(memberAdapter);
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(recyclerViewMembers);
        }
    }

    private void loadMembersData() {
        if (currentRound == null || currentRound.getMembers() == null || currentRound.getMembers().isEmpty()) {
            // No members or error
            return;
        }

        // Query Firestore for each member's details
        for (String memberId : currentRound.getMembers()) {
            db.collection("users").document(memberId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    User user = document.toObject(User.class);
                                    if (user != null) {
                                        user.setId(document.getId());
                                        membersList.add(user);
                                        memberAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        }
                    });
        }
    }

    private void setupClickListeners() {
        btnAddNewUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToAddUsers();
            }
        });

        btnInviteUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareInviteCode();
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
                        navigateToManageRound();
                        break;
                    case 2: // Members tab
                        // Already on members tab, no action needed
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

    private void savePaymentOrder() {
        if (currentRound == null || membersList.isEmpty()) return;

        // Create payment order mapping (userId -> position)
        Map<String, Integer> payoutOrder = new HashMap<>();
        for (int i = 0; i < membersList.size(); i++) {
            payoutOrder.put(membersList.get(i).getId(), i);
        }

        // Update round object
        currentRound.setPayoutOrder(payoutOrder);

        // Save to Firestore
        db.collection("rounds").document(currentRound.getId())
                .update("payoutOrder", payoutOrder)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(requireContext(), "Payment order updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireContext(), "Error updating payment order: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeMember(User user, int position) {
        if (currentRound == null || user == null) return;

        // Remove from local list
        membersList.remove(position);
        memberAdapter.notifyItemRemoved(position);

        // Remove from round's members list
        List<String> updatedMembers = currentRound.getMembers();
        updatedMembers.remove(user.getId());

        // Update Firestore
        db.collection("rounds").document(currentRound.getId())
                .update("members", updatedMembers)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Record activity
                        recordMemberRemoved(user);
                        Toast.makeText(requireContext(), user.getName() + " removed from round",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Revert local change on error
                        membersList.add(position, user);
                        memberAdapter.notifyItemInserted(position);

                        Toast.makeText(requireContext(), "Error removing member: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void recordMemberRemoved(User user) {
        if (currentRound == null || currentUser == null || user == null) return;

        // Create activity record
        Map<String, Object> activity = new HashMap<>();
        activity.put("type", "member_removed");
        activity.put("userId", user.getId());
        activity.put("userName", user.getName());
        activity.put("message", user.getName() + " left the round");
        activity.put("timestamp", new Date());

        // Add to Firestore
        db.collection("rounds")
                .document(currentRound.getId())
                .collection("activities")
                .add(activity);
    }

    private void navigateToAddUsers() {
        Fragment addUsersFragment = new addUsers();

        // Pass the round data
        Bundle args = new Bundle();
        args.putSerializable("selected_round", currentRound);
        addUsersFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, addUsersFragment)
                .addToBackStack(null)
                .commit();
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

    private void navigateToManageRound() {
        Fragment manageRoundFragment = new manageRound();

        // Pass the round data
        Bundle args = new Bundle();
        args.putSerializable("selected_round", currentRound);
        manageRoundFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, manageRoundFragment)
                .addToBackStack(null)
                .commit();
    }

    private void goBack() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    // Model class for User
    public static class User {
        private String id;
        private String name;
        private String email;
        private String phoneNumber;

        public User() {
            // Required empty constructor for Firestore
        }

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }

    // Adapter for Members RecyclerView
    private class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder>
            implements MemberItemTouchHelperCallback.ItemTouchHelperAdapter {

        private List<User> users;

        public MemberAdapter(List<User> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_member, parent, false);
            return new MemberViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
            User user = users.get(position);
            holder.bind(user, position);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        @Override
        public void onItemMove(int fromPosition, int toPosition) {
            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(users, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(users, i, i - 1);
                }
            }
            notifyItemMoved(fromPosition, toPosition);

            // Save the new order after drag and drop
            savePaymentOrder();
        }

        @Override
        public void onItemDismiss(int position) {
            // Remove member functionality
            User userToRemove = users.get(position);
            removeMember(userToRemove, position);
        }

        class MemberViewHolder extends RecyclerView.ViewHolder {
            TextView nameTextView;
            TextView emailTextView;
            TextView payoutOrderTextView;
            Button removeButton;

            public MemberViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.memberNameTextView);
                emailTextView = itemView.findViewById(R.id.memberEmailTextView);
                payoutOrderTextView = itemView.findViewById(R.id.payoutOrderTextView);
                removeButton = itemView.findViewById(R.id.removeMemberButton);
            }

            public void bind(final User user, final int position) {
                nameTextView.setText(user.getName());
                emailTextView.setText(user.getEmail());

                // Set payout order text
                if ("custom".equals(currentRound.getPayoutOrderType())) {
                    payoutOrderTextView.setText("Payout order: #" + (position + 1));
                    payoutOrderTextView.setVisibility(View.VISIBLE);
                } else {
                    payoutOrderTextView.setVisibility(View.GONE);
                }

                // Remove button functionality
                removeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeMember(user, position);
                    }
                });
            }
        }
    }

    // Touch helper callback for drag and drop functionality
    public static class MemberItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private final ItemTouchHelperAdapter adapter;

        public MemberItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false; // We handle removal with a button
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // Not used as we don't enable swiping
        }

        public interface ItemTouchHelperAdapter {
            void onItemMove(int fromPosition, int toPosition);
            void onItemDismiss(int position);
        }
    }
}