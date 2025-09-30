package com.example.rounds_fyp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class faq_page extends Fragment {

    ExpandableListView faqListView;
    List<String> faqQuestions;
    HashMap<String, List<String>> faqAnswers;
    Button btnContact;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faq_page, container, false);

        faqListView = view.findViewById(R.id.faqListView);
        btnContact = view.findViewById(R.id.btnContact);

        setupFAQData();

        FAQExpandableListAdapter adapter = new FAQExpandableListAdapter(getContext(), faqQuestions, faqAnswers);
        faqListView.setAdapter(adapter);

        btnContact.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:charumbirafarirai@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Rounds App Support");
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        });

        return view;
    }

    private void setupFAQData() {
        faqQuestions = new ArrayList<>();
        faqAnswers = new HashMap<>();

        faqQuestions.add("How do I make a contribution?");
        faqQuestions.add("How do I verify my account?");
        faqQuestions.add("How do I join a round?");
        faqQuestions.add("Can I leave a round?");
        faqQuestions.add("How are payouts decided?");
        faqQuestions.add("What payment methods are supported?");

        faqAnswers.put(faqQuestions.get(0), List.of("Go to the round, click 'Add Contribution', enter the amount and proceed to payment."));
        faqAnswers.put(faqQuestions.get(1), List.of("You can verify your account via email link sent after registration."));
        faqAnswers.put(faqQuestions.get(2), List.of("You can join using a link shared by the group admin or by being added manually."));
        faqAnswers.put(faqQuestions.get(3), List.of("Yes, but only before a round has started. Contact your group admin."));
        faqAnswers.put(faqQuestions.get(4), List.of("Payout order is defined by the group creator as either manual or random."));
        faqAnswers.put(faqQuestions.get(5), List.of("Bank transfer and cash pickup are currently supported."));

    }
}
