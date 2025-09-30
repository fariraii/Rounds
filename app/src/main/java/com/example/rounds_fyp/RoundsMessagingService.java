package com.example.rounds_fyp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RoundsMessagingService extends FirebaseMessagingService {

    private static final String TAG = "RoundsMessagingService";
    private static final String CHANNEL_ID = "rounds_notifications";
    private static final String CHANNEL_NAME = "Rounds Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for Rounds app";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());

            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            // Handle the notification
            sendNotification(title, body, remoteMessage.getData());
        }

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // Handle data payload
            Map<String, String> data = remoteMessage.getData();

            // If data contains specific instructions
            if (data.containsKey("type")) {
                String type = data.get("type");

                switch(type) {
                    case "contribution_reminder":
                        handleContributionReminder(data);
                        break;
                    case "payout_received":
                        handlePayoutReceived(data);
                        break;
                    case "new_member":
                        handleNewMember(data);
                        break;
                    default:
                        // Default handling for unknown types
                        sendNotification(
                                data.getOrDefault("title", "Rounds Update"),
                                data.getOrDefault("body", "You have a new update in Rounds"),
                                data
                        );
                        break;
                }
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // Save new token to Firestore for the current user
        saveTokenToFirestore(token);
    }

    private void saveTokenToFirestore(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("fcmToken", token);
            tokenData.put("updatedAt", System.currentTimeMillis());

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update(tokenData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully updated"))
                    .addOnFailureListener(e -> Log.w(TAG, "Error updating token", e));
        }
    }

    private void handleContributionReminder(Map<String, String> data) {
        String roundName = data.getOrDefault("roundName", "your round");
        String contributionDate = data.getOrDefault("contributionDate", "soon");
        String amount = data.getOrDefault("amount", "");

        String title = "Contribution Reminder";
        String body = "Reminder: Your contribution of " + amount + " for " + roundName + " is due on " + contributionDate;

        sendNotification(title, body, data);
    }

    private void handlePayoutReceived(Map<String, String> data) {
        String roundName = data.getOrDefault("roundName", "your round");
        String amount = data.getOrDefault("amount", "");

        String title = "Payout Received";
        String body = "Great news! You've received a payout of " + amount + " from " + roundName;

        sendNotification(title, body, data);
    }

    private void handleNewMember(Map<String, String> data) {
        String roundName = data.getOrDefault("roundName", "your round");
        String memberName = data.getOrDefault("memberName", "A new member");

        String title = "New Member Joined";
        String body = memberName + " has joined " + roundName;

        sendNotification(title, body, data);
    }

    private void sendNotification(String title, String messageBody, Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);

        // Add data to intent if round ID is provided
        if (data.containsKey("roundId")) {
            intent.putExtra("roundId", data.get("roundId"));
            intent.putExtra("openRound", true);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Use flags for proper API compatibility
        int flags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        // Notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.rounds_logo)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESCRIPTION);
            notificationManager.createNotificationChannel(channel);
        }

        // Use a unique ID for each notification
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}