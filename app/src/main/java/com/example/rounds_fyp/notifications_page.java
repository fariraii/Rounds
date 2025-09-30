package com.example.rounds_fyp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class notifications_page extends Fragment {

    private ListView notificationsListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notifications_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationsListView = view.findViewById(R.id.notificationsListView);

        ArrayList<String> notifications = new ArrayList<>();
        notifications.add("Your next contribution is due tomorrow · 1 day ago");
        notifications.add("You received a payout from Round A · 3 days ago");
        notifications.add("Member Sandra joined your group · 4 days ago");
        notifications.add("New round 'Holiday Savings' was created · 1 week ago");
        notifications.add("Contribution reminder sent to group · 1 week ago");
        notifications.add("Profile updated successfully · 2 weeks ago");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                notifications
        );

        notificationsListView.setAdapter(adapter);
    }
}
