package com.example.rounds_fyp;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

public class settings extends Fragment {

    private CheckBox allowNotificationsCheckbox;
    private CheckBox darkModeCheckbox;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AppSettingsPrefs";
    private static final String NOTIFICATIONS_KEY = "allow_notifications";
    private static final String DARK_MODE_KEY = "dark_mode";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize UI elements
        allowNotificationsCheckbox = view.findViewById(R.id.allowNotificationsCheckbox);
        darkModeCheckbox = view.findViewById(R.id.darkModeCheckbox);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, getContext().MODE_PRIVATE);

        // Load saved preferences
        loadPreferences();

        // Set listeners for the checkboxes
        allowNotificationsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveNotificationPreference(isChecked)
        );

        darkModeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveDarkModePreference(isChecked)
        );

        return view;
    }

    private void loadPreferences() {
        boolean allowNotifications = sharedPreferences.getBoolean(NOTIFICATIONS_KEY, false);
        boolean darkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false);

        allowNotificationsCheckbox.setChecked(allowNotifications);
        darkModeCheckbox.setChecked(darkMode);
    }

    private void saveNotificationPreference(boolean isEnabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(NOTIFICATIONS_KEY, isEnabled);
        editor.apply();
    }

    private void saveDarkModePreference(boolean isEnabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DARK_MODE_KEY, isEnabled);
        editor.apply();

        // Here you could also implement the logic to switch to dark mode
        // For example, by using AppCompatDelegate.setDefaultNightMode()
    }
}