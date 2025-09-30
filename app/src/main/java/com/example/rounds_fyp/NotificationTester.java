package com.example.rounds_fyp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to test notifications without waiting for Firebase Cloud Messaging
 */
public class NotificationTester {
    private static final String TAG = "NotificationTester";

    /**
     * Create and display a local notification with the same structure as a Firebase notification
     * @param context Application context
     * @param title Notification title
     * @param message Notification message
     * @param data Additional data to include
     */
    public static void createLocalNotification(Context context, String title, String message, Map<String, String> data) {
        // Create notification channel for Android O and above
        createNotificationChannel(context);

        // Create intent for notification tap action
        Intent intent = new Intent(context, MainActivity.class);

        // Add data from the notification
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        // Set flags for proper navigation
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Create pending intent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);

        // Get notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, "firebase")
                        .setSmallIcon(R.drawable.rounds_logo)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Show notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Check notification permission
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            // Use a unique ID for each notification based on current time
            int notificationId = (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, notificationBuilder.build());

            Log.d(TAG, "Local notification displayed: " + title);
        } else {
            Log.e(TAG, "Notification permission not granted");
        }
    }

    /**
     * Creates a test contribution notification
     * @param context Application context
     * @param userName User who made the contribution
     * @param amount Contribution amount
     * @param roundName Round name
     * @param roundId Round ID
     */
    public static void testContributionNotification(Context context, String userName,
                                                    double amount, String roundName, String roundId) {
        String title = "New Contribution";
        String message = userName + " contributed £" + String.format("%.2f", amount) +
                " to " + roundName;

        Map<String, String> data = new HashMap<>();
        data.put("type", "contribution_made");
        data.put("roundId", roundId);
        data.put("openRound", "true");

        createLocalNotification(context, title, message, data);
    }

    /**
     * Creates a test payment reminder notification
     * @param context Application context
     * @param amount Payment amount
     * @param roundName Round name
     * @param dueDate Due date (formatted string)
     * @param roundId Round ID
     */
    public static void testPaymentReminderNotification(Context context, double amount,
                                                       String roundName, String dueDate, String roundId) {
        String title = "Payment Reminder";
        String message = "Your payment of £" + String.format("%.2f", amount) +
                " for " + roundName + " is due on " + dueDate;

        Map<String, String> data = new HashMap<>();
        data.put("type", "payment_reminder");
        data.put("roundId", roundId);
        data.put("openRound", "true");

        createLocalNotification(context, title, message, data);
    }

    /**
     * Create notification channel for Android O and above
     * @param context Application context
     */
    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Firebase Notifications";
            String description = "Channel for Firebase Cloud Messaging";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel("firebase", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}