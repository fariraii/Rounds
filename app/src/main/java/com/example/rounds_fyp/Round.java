package com.example.rounds_fyp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Model class to represent a savings round
 */
public class Round implements Serializable {
    private String id;
    private String name;
    private double contributionAmount;
    private int contributionDate;
    private String payoutOrderType;
    private int durationMonths;
    private String creatorId;
    private Date createdAt;
    private List<String> members;
    private Map<String, Integer> payoutOrder;
    private boolean isActive;

    /**
     * Empty constructor needed for Firebase
     */
    public Round() {
        // Initialize default values
        this.members = new ArrayList<>();
        this.isActive = true;
        this.createdAt = new Date();
    }

    /**
     * Constructor with all fields
     */
    public Round(String id, String name, double contributionAmount, int contributionDate,
                 String payoutOrderType, int durationMonths, String creatorId) {
        this.id = id;
        this.name = name;
        this.contributionAmount = contributionAmount;
        this.contributionDate = contributionDate;
        this.payoutOrderType = payoutOrderType;
        this.durationMonths = durationMonths;
        this.creatorId = creatorId;
        this.createdAt = new Date();
        this.members = new ArrayList<>();
        this.isActive = true;
    }

    /**
     * Constructor with most fields but generated ID
     */
    public Round(String name, double contributionAmount, int contributionDate,
                 String payoutOrderType, int durationMonths, String creatorId) {
        this(null, name, contributionAmount, contributionDate, payoutOrderType,
                durationMonths, creatorId);
    }

    // Getters and Setters
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

    public double getContributionAmount() {
        return contributionAmount;
    }

    public void setContributionAmount(double contributionAmount) {
        this.contributionAmount = contributionAmount;
    }

    public int getContributionDate() {
        return contributionDate;
    }

    public void setContributionDate(int contributionDate) {
        this.contributionDate = contributionDate;
    }

    public String getPayoutOrderType() {
        return payoutOrderType;
    }

    public void setPayoutOrderType(String payoutOrderType) {
        this.payoutOrderType = payoutOrderType;
    }

    public int getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(int durationMonths) {
        this.durationMonths = durationMonths;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public Map<String, Integer> getPayoutOrder() {
        return payoutOrder;
    }

    public void setPayoutOrder(Map<String, Integer> payoutOrder) {
        this.payoutOrder = payoutOrder;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // Helper methods
    /**
     * Adds a member to the round if they don't already exist in the members list
     * @param userId The user ID to add
     */
    public void addMember(String userId) {
        if (members == null) {
            members = new ArrayList<>();
        }

        if (!members.contains(userId)) {
            members.add(userId);
        }
    }

    /**
     * Removes a member from the round
     * @param userId The user ID to remove
     * @return true if the member was removed, false otherwise
     */
    public boolean removeMember(String userId) {
        if (members != null) {
            return members.remove(userId);
        }
        return false;
    }

    /**
     * Gets the number of members in this round
     * @return The member count
     */
    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }

    /**
     * Checks if a user is a member of this round
     * @param userId The user ID to check
     * @return true if the user is a member, false otherwise
     */
    public boolean isMember(String userId) {
        return members != null && members.contains(userId);
    }

    /**
     * Checks if a user is the creator of this round
     * @param userId The user ID to check
     * @return true if the user is the creator, false otherwise
     */
    public boolean isCreator(String userId) {
        return userId != null && userId.equals(creatorId);
    }

    /**
     * Gets the next payout date based on the contribution date
     * @return A string representation of the next payout date
     */
    public String getNextPayoutDate() {
        // Simple calculation of next payout date - 7 days after contribution
        return "Day " + Math.min(contributionDate + 7, 30) + " of the month";
    }

    /**
     * Gets the total amount that will be collected in this round
     * @return The total amount
     */
    public double getTotalAmount() {
        return contributionAmount * getMemberCount();
    }

    /**
     * Converts this Round to a Map for Firebase storage
     * @return A Map representation of this Round
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("contributionAmount", contributionAmount);
        map.put("contributionDate", contributionDate);
        map.put("payoutOrderType", payoutOrderType);
        map.put("durationMonths", durationMonths);
        map.put("creatorId", creatorId);
        map.put("createdAt", createdAt);
        map.put("members", members);
        map.put("payoutOrder", payoutOrder);
        map.put("isActive", isActive);
        return map;
    }

    @Override
    public String toString() {
        return "Round{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", contributionAmount=" + contributionAmount +
                ", contributionDate=" + contributionDate +
                ", payoutOrderType='" + payoutOrderType + '\'' +
                ", durationMonths=" + durationMonths +
                ", memberCount=" + getMemberCount() +
                '}';
    }
}