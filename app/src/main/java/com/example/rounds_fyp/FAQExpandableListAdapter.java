package com.example.rounds_fyp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

public class FAQExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> faqQuestions;
    private HashMap<String, List<String>> faqAnswers;

    public FAQExpandableListAdapter(Context context, List<String> faqQuestions, HashMap<String, List<String>> faqAnswers) {
        this.context = context;
        this.faqQuestions = faqQuestions;
        this.faqAnswers = faqAnswers;
    }

    @Override
    public int getGroupCount() {
        return faqQuestions.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return faqAnswers.get(faqQuestions.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return faqQuestions.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return faqAnswers.get(faqQuestions.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    // Group (Question)
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String question = (String) getGroup(groupPosition);

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.faq_group_item, parent, false);
        }

        TextView questionText = convertView.findViewById(R.id.faqQuestion);
        questionText.setText(question);

        return convertView;
    }

    // Child (Answer)
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        String answer = (String) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.faq_answer_item, parent, false);
        }

        TextView answerText = convertView.findViewById(R.id.faq_answer);
        answerText.setText(answer);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
