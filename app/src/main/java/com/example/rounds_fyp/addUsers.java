package com.example.rounds_fyp;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

public class addUsers extends Fragment {

    private static final int REQUEST_READ_CONTACTS = 1;

    // UI components
    private Button copyLinkButton;
    private ListView contactsListView;
    private Button addSelectedButton;

    // Data
    private Round currentRound;
    private List<Contact> contactsList;
    private List<Contact> selectedContacts;
    private ContactAdapter contactAdapter;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_users, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI components
        copyLinkButton = view.findViewById(R.id.copyLinkButton);
        contactsListView = view.findViewById(R.id.contactsListView);
        addSelectedButton = view.findViewById(R.id.addSelectedButton);

        // Initialize data structures
        contactsList = new ArrayList<>();
        selectedContacts = new ArrayList<>();

        // Get the round from arguments
        Bundle args = getArguments();
        if (args != null && args.containsKey("selected_round")) {
            currentRound = (Round) args.getSerializable("selected_round");
        } else {
            // Handle case where round data is missing
            Toast.makeText(requireContext(), "Error: Round details not available", Toast.LENGTH_SHORT).show();
            goBack();
        }

        // Set up the contacts list
        setupContactsList();

        // Request contacts permission if needed
        requestContactsPermission();

        // Set up click listeners
        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        copyLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyInviteLinkToClipboard();
            }
        });

        addSelectedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSelectedContactsToRound();
            }
        });
    }

    private void setupContactsList() {
        contactAdapter = new ContactAdapter(requireContext(), contactsList);
        contactsListView.setAdapter(contactAdapter);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Contact contact = contactsList.get(position);
                contact.setSelected(!contact.isSelected());

                if (contact.isSelected()) {
                    selectedContacts.add(contact);
                } else {
                    selectedContacts.remove(contact);
                }

                contactAdapter.notifyDataSetChanged();
            }
        });
    }

    private void requestContactsPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_CONTACTS},
                    REQUEST_READ_CONTACTS);
        } else {
            loadContacts();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            } else {
                Toast.makeText(requireContext(), "Permission denied to read contacts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadContacts() {
        contactsList.clear();

        // Get phone contacts
        Cursor cursor = requireActivity().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                // Create contact object
                Contact contact = new Contact(name, phoneNumber, null);

                // Check if this contact is already a member of the round
                if (currentRound != null && currentRound.getMembers() != null) {
                    for (String memberId : currentRound.getMembers()) {
                        // In a real app, you'd need to check if the phone number matches a user account
                        // This is simplified for demonstration
                    }
                }

                // Add to list if not already a member
                if (!contact.isMember()) {
                    contactsList.add(contact);
                }
            }
            cursor.close();
        }

        // Also query users from Firebase (who are registered in the app)
        db.collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String userId = document.getId();
                                String name = document.getString("name");
                                String email = document.getString("email");
                                String phone = document.getString("phoneNumber");

                                // Skip current user and users already in the round
                                if (currentUser != null && userId.equals(currentUser.getUid())) {
                                    continue;
                                }

                                if (currentRound != null && currentRound.getMembers() != null &&
                                        currentRound.getMembers().contains(userId)) {
                                    continue;
                                }

                                // Create contact object for app user
                                Contact contact = new Contact(name, phone, email);
                                contact.setUserId(userId);
                                contact.setAppUser(true);

                                contactsList.add(contact);
                            }

                            // Update the adapter
                            contactAdapter.notifyDataSetChanged();
                        }
                    }
                });

        // Update the adapter
        contactAdapter.notifyDataSetChanged();
    }

    private void copyInviteLinkToClipboard() {
        if (currentRound == null) return;

        // Generate invite link (in a real app, this would be a deep link)
        String inviteLink = "https://rounds.app/join?id=" + currentRound.getId();

        // Copy to clipboard
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Rounds Invite Link", inviteLink);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(requireContext(), "Invite link copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void addSelectedContactsToRound() {
        if (currentRound == null || selectedContacts.isEmpty()) {
            Toast.makeText(requireContext(), "Please select contacts to add", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the list of current members
        List<String> updatedMembers = new ArrayList<>();
        if (currentRound.getMembers() != null) {
            updatedMembers.addAll(currentRound.getMembers());
        }

        // Add app users to the round (those who have user IDs)
        int addedCount = 0;
        for (Contact contact : selectedContacts) {
            if (contact.isAppUser() && contact.getUserId() != null) {
                if (!updatedMembers.contains(contact.getUserId())) {
                    updatedMembers.add(contact.getUserId());
                    addedCount++;

                    // Record activity for this new member
                    recordMemberAdded(contact);
                }
            } else {
                // For non-app users, we would typically send an SMS invite
                // This is simplified for demonstration
                sendSMSInvite(contact);
            }
        }

        // Update Firestore with new members list
        if (addedCount > 0) {
            int finalAddedCount = addedCount;
            db.collection("rounds").document(currentRound.getId())
                    .update("members", updatedMembers)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Update the local round object
                            currentRound.setMembers(updatedMembers);

                            Toast.makeText(requireContext(), finalAddedCount + " members added to the round",
                                    Toast.LENGTH_SHORT).show();

                            // Navigate back to round activity
                            navigateToRoundActivity();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(requireContext(), "Error adding members: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // If we only sent SMS invites but didn't add anyone directly
            if (selectedContacts.size() > 0) {
                Toast.makeText(requireContext(), "Invites sent to selected contacts",
                        Toast.LENGTH_SHORT).show();
                navigateToRoundActivity();
            } else {
                Toast.makeText(requireContext(), "No contacts were added",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void recordMemberAdded(Contact contact) {
        if (currentRound == null || currentUser == null || contact == null) return;

        // Create activity record
        Map<String, Object> activity = new HashMap<>();
        activity.put("type", "member_added");
        activity.put("userId", contact.getUserId());
        activity.put("userName", contact.getName());
        activity.put("message", contact.getName() + " joined the round");
        activity.put("timestamp", new Date());

        // Add to Firestore
        db.collection("rounds")
                .document(currentRound.getId())
                .collection("activities")
                .add(activity);
    }

    private void sendSMSInvite(Contact contact) {
        // In a real app, you would integrate with SMS API
        // This is simplified for demonstration
        Toast.makeText(requireContext(), "SMS invite would be sent to " + contact.getName(),
                Toast.LENGTH_SHORT).show();
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

    private void goBack() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    // Model class for Contact
    public static class Contact {
        private String name;
        private String phoneNumber;
        private String email;
        private String userId;
        private boolean isAppUser;
        private boolean isMember;
        private boolean isSelected;

        public Contact(String name, String phoneNumber, String email) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.email = email;
            this.isAppUser = false;
            this.isMember = false;
            this.isSelected = false;
        }

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public boolean isAppUser() {
            return isAppUser;
        }

        public void setAppUser(boolean appUser) {
            isAppUser = appUser;
        }

        public boolean isMember() {
            return isMember;
        }

        public void setMember(boolean member) {
            isMember = member;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }
    }

    // Adapter for Contacts ListView
    private class ContactAdapter extends ArrayAdapter<Contact> {
        private Context context;
        private List<Contact> contacts;

        public ContactAdapter(Context context, List<Contact> contacts) {
            super(context, 0, contacts);
            this.context = context;
            this.contacts = contacts;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
            }

            Contact contact = getItem(position);
            if (contact != null) {
                TextView nameTextView = convertView.findViewById(R.id.contactNameTextView);
                TextView detailTextView = convertView.findViewById(R.id.contactDetailTextView);
                CheckBox selectCheckBox = convertView.findViewById(R.id.contactCheckBox);

                // Set data to views
                nameTextView.setText(contact.getName());

                // Show email for app users, phone number for contacts
                if (contact.isAppUser()) {
                    detailTextView.setText(contact.getEmail());
                } else {
                    detailTextView.setText(contact.getPhoneNumber());
                }

                // Set checkbox state
                selectCheckBox.setChecked(contact.isSelected());

                // Set checkbox click listener
                selectCheckBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        contact.setSelected(selectCheckBox.isChecked());
                        if (contact.isSelected()) {
                            selectedContacts.add(contact);
                        } else {
                            selectedContacts.remove(contact);
                        }
                    }
                });
            }

            return convertView;
        }
    }
}