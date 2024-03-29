package com.example.bluetooth_connect;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class SyncService extends Service {
    private static final String TAG = "SyncService";
    private static final int NOTIFICATION_ID = 1234;
    private static final long NOTIFICATION_INTERVAL = 3 * 1000; // 15 seconds

    private PowerManager.WakeLock wakeLock;
    private Handler handler;
    private Runnable notificationRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Sync Service created");

        // Acquire wake lock to keep the CPU running
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "SyncService::WakeLock");
        wakeLock.acquire();

        // Check for permissions if running on Android 9 (Pie) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //TODO Change the part of the notification for the Sync Data function

        // Create a notification for the foreground service
        Notification notification = createNotification();
        if (notification != null) {
            startForeground(NOTIFICATION_ID, notification);
        }

        // Initialize Handler and Runnable for periodic notification update
        handler = new Handler();
        notificationRunnable = new Runnable() {
            @Override
            public void run() {
                updateNotification();
                handler.postDelayed(this, NOTIFICATION_INTERVAL);
            }
        };
        handler.postDelayed(notificationRunnable, NOTIFICATION_INTERVAL);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Sync Service started");
        // Implement your data sync logic here
        syncData();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Sync Service destroyed");

        // Release the wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Stop the periodic notification update
        if (handler != null && notificationRunnable != null) {
            handler.removeCallbacks(notificationRunnable);
        }

        // Stop the foreground service and remove the notification
        stopForeground(true);
    }

    private void syncData() {
        // Implement your data sync logic here
        // This could involve network calls, updating the SQLite database, etc.
        //TODO Verify if it has internet connection

        //TODO Make database connection
        Log.d(TAG, "Syncing data...");
    }

    private void updateNotification() {
        //Log.d(TAG, "Updating notification...");
        Notification notification = createNotification();
        if (notification != null) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "ChannelID")
                .setContentTitle("Sync Service")
                .setContentText("Sync Service is running...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);

        // Check if the device is running Android 8.0 (Oreo) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "ChannelID",
                    "Sync Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Sync Service Channel Description");

            // Register the channel with the system; you can't change the importance or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            builder.setChannelId("ChannelID"); // Set the channel ID for Android Oreo and above
        }

        return builder.build();
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}