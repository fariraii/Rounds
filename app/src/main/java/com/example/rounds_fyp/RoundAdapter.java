package com.example.rounds_fyp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Custom adapter for displaying Round items in a ListView
 */
public class RoundAdapter extends ArrayAdapter<Round> {
    private Context context;
    private List<Round> rounds;

    public RoundAdapter(@NonNull Context context, List<Round> rounds) {
        super(context, 0, rounds);
        this.context = context;
        this.rounds = rounds;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_round, parent, false);
        }

        Round round = getItem(position);
        if (round != null) {
            TextView roundNameTextView = convertView.findViewById(R.id.roundNameTextView);
            TextView memberCountTextView = convertView.findViewById(R.id.memberCountTextView);
            TextView contributionAmountTextView = convertView.findViewById(R.id.contributionAmountTextView);
            TextView nextPayoutTextView = convertView.findViewById(R.id.nextPayoutTextView);

            // Set data to views
            roundNameTextView.setText(round.getName());
            memberCountTextView.setText(round.getMemberCount() + " members");

            // Format currency
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            contributionAmountTextView.setText(currencyFormat.format(round.getContributionAmount()) + " monthly");

            nextPayoutTextView.setText("Next payout: " + round.getNextPayoutDate());
        }

        return convertView;
    }
}