package com.example.rounds_fyp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ActiveRoundsAdapter extends RecyclerView.Adapter<ActiveRoundsAdapter.RoundViewHolder> {

    private Context context;
    private List<Round> rounds;
    private FragmentManager fragmentManager;

    public ActiveRoundsAdapter(Context context, List<Round> rounds) {
        this.context = context;
        this.rounds = rounds;
        if (context instanceof MainActivity) {
            this.fragmentManager = ((MainActivity) context).getSupportFragmentManager();
        }
    }

    @NonNull
    @Override
    public RoundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_active_round, parent, false);
        return new RoundViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoundViewHolder holder, int position) {
        Round round = rounds.get(position);
        holder.bind(round);

        // Set click listener for the whole item
        holder.itemView.setOnClickListener(v -> {
            if (fragmentManager != null) {
                // Navigate to round activity
                navigateToRoundActivity(round);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rounds.size();
    }

    private void navigateToRoundActivity(Round round) {
        // Create round activity fragment
        roundActivity roundActivityFragment = new roundActivity();

        // Pass the round data via bundle
        Bundle args = new Bundle();
        args.putSerializable("selected_round", round);
        roundActivityFragment.setArguments(args);

        // Navigate to the fragment
        fragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, roundActivityFragment)
                .addToBackStack(null)
                .commit();
    }

    static class RoundViewHolder extends RecyclerView.ViewHolder {
        TextView roundName;
        TextView roundAmount;
        TextView memberCount;
        TextView nextPayment;

        public RoundViewHolder(@NonNull View itemView) {
            super(itemView);
            roundName = itemView.findViewById(R.id.roundName);
            roundAmount = itemView.findViewById(R.id.roundAmount);
            memberCount = itemView.findViewById(R.id.memberCount);
            nextPayment = itemView.findViewById(R.id.nextPayment);
        }

        public void bind(Round round) {
            roundName.setText(round.getName());

            // Format currency
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.UK);
            roundAmount.setText(currencyFormat.format(round.getContributionAmount()));

            // Set member count
            int memberCount = round.getMembers() != null ? round.getMembers().size() : 0;
            this.memberCount.setText(memberCount + " members");

            // Calculate next payment date based on contribution date setting in the round
            Calendar calendar = Calendar.getInstance();
            int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
            int contributionDate = round.getContributionDate();

            // If today is past the contribution date, show next month's date
            if (currentDay > contributionDate) {
                calendar.add(Calendar.MONTH, 1);
            }

            // Set the day to the contribution date
            calendar.set(Calendar.DAY_OF_MONTH, contributionDate);

            // Format the date (Apr 25)
            String month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            nextPayment.setText("Next payment: " + month + " " + day);
        }
    }
}