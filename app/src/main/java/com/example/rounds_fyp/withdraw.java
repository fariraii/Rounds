package com.example.rounds_fyp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

public class withdraw extends Fragment {
    private TextView currentBalanceAmount;
    private TextInputEditText withdrawAmountInput;
    private Button withdrawButton;
    private ListView paymentMethodList;

    // Constants for SharedPreferences
    private static final String PREF_NAME = "RoundsPreferences";
    private static final String BALANCE_KEY = "currentBalance";

    // Default balance if no balance is stored yet
    private static final double DEFAULT_BALANCE = 267.0;

    private double currentBalance;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_withdraw, container, false);

        // Initialize UI components
        currentBalanceAmount = view.findViewById(R.id.currentBalanceAmount);
        withdrawAmountInput = view.findViewById(R.id.withdrawAmountInput);
        withdrawButton = view.findViewById(R.id.withdrawButton);
        paymentMethodList = view.findViewById(R.id.paymentMethodList);

        // Load balance from SharedPreferences
        loadBalance();

        // Set up payment methods
        setupPaymentMethods();

        // Set up withdraw button click listener
        setupWithdrawButton();

        return view;
    }

    private void loadBalance() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentBalance = prefs.getFloat(BALANCE_KEY, (float) DEFAULT_BALANCE);
        updateBalanceDisplay();
    }

    private void saveBalance() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(BALANCE_KEY, (float) currentBalance);
        editor.apply();
    }

    private void updateBalanceDisplay() {
        // Format balance with $ sign and 2 decimal places
        currentBalanceAmount.setText(String.format("£%.2f", currentBalance));
    }

    private void setupPaymentMethods() {
        // Example payment methods
        String[] paymentMethods = {"Bank Transfer", "Mobile Money", "Cash"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_single_choice, paymentMethods);

        paymentMethodList.setAdapter(adapter);
        paymentMethodList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        paymentMethodList.setItemChecked(0, true); // Set first item as default
    }

    private void setupWithdrawButton() {
        withdrawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processWithdrawal();
            }
        });
    }

    private void processWithdrawal() {
        String amountStr = withdrawAmountInput.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse the amount
        double withdrawAmount;
        try {
            withdrawAmount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if amount is positive
        if (withdrawAmount <= 0) {
            Toast.makeText(requireContext(), "Please enter an amount greater than zero", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if sufficient balance is available
        if (withdrawAmount > currentBalance) {
            Toast.makeText(requireContext(), "Insufficient balance", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if payment method is selected
        int selectedPaymentMethod = paymentMethodList.getCheckedItemPosition();
        if (selectedPaymentMethod == ListView.INVALID_POSITION) {
            Toast.makeText(requireContext(), "Please select a payment method", Toast.LENGTH_SHORT).show();
            return;
        }

        // Process withdrawal
        currentBalance -= withdrawAmount;
        updateBalanceDisplay();
        saveBalance();

        // Show success message
        Toast.makeText(requireContext(),
                String.format("Withdrawal of £%.2f successful", withdrawAmount),
                Toast.LENGTH_SHORT).show();

        // Notify the BalanceUpdateListener if any fragment is listening
        if (getActivity() instanceof BalanceUpdateListener) {
            ((BalanceUpdateListener) getActivity()).onBalanceUpdated(currentBalance);
        }

        // Clear input field
        withdrawAmountInput.setText("");

        // Navigate back to the previous screen
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    // Interface for balance update callback
    public interface BalanceUpdateListener {
        void onBalanceUpdated(double newBalance);
    }
}