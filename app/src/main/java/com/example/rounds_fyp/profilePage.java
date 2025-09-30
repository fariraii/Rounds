package com.example.rounds_fyp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class profilePage extends Fragment {
    private ListView profileOptionsListView;
    private Button btnLogout;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_page, container, false);

        profileOptionsListView = view.findViewById(R.id.profileOptionsList);

        // Profile options
        String[] options = {"Settings", "Personal Details", "Get Help"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, options) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView item = (TextView) super.getView(position, convertView, parent);
                item.setTextColor(getResources().getColor(android.R.color.black));  // Ensure black text
                item.setTextSize(16);  // Optional - make sure text is readable
                return item;
            }
        };

        profileOptionsListView.setAdapter(adapter);

        // Navigation logic
        profileOptionsListView.setOnItemClickListener((parent, view1, position, id) -> {
            switch (position) {
                case 0:
                    Navigation.findNavController(view).navigate(R.id.settings);
                    break;
                case 1:
                    Navigation.findNavController(view).navigate(R.id.personalDetails);
                    break;
                case 2:
                    Navigation.findNavController(view).navigate(R.id.faqPage);
                    break;
            }
        });

        // Initialize FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();

        // Find the logout button
        btnLogout = view.findViewById(R.id.btnLogout);

        // Set up logout functionality
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut(); // Sign out the user

            // Redirect to login screen
            Intent intent = new Intent(getActivity(), login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // clear backstack
            startActivity(intent);
        });

        TextView nameView = view.findViewById(R.id.userName);
        TextView memberSinceView = view.findViewById(R.id.memberSince);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String fullName = document.getString("fullName");

                            // Set full name
                            if (fullName != null) nameView.setText(fullName);


                            // Format and set member since
                            if (document.contains("createdAt")) {
                                long createdAt = document.getLong("createdAt");
                                Date createdDate = new Date(createdAt);
                                SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                                memberSinceView.setText("Member since: " + sdf.format(createdDate));
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Failed to fetch user data", e));
        }



        return view;
    }
    @Override
    public void onStart() {
        super.onStart();

        // Optional: Check if the user is already logged out
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // If no user is signed in, go to login screen
            Intent intent = new Intent(getActivity(), login.class);
            startActivity(intent);
            requireActivity().finish();
        }
    }
}