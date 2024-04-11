package com.example.bluetooth_connect;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class LocationForegroundService extends Service {
    private LocationHelper locationHelper;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("destinationLat") && intent.hasExtra("destinationLng")) {
            double destinationLat = intent.getDoubleExtra("destinationLat", 0.0);
            double destinationLng = intent.getDoubleExtra("destinationLng", 0.0);
            String macAddress = intent.getStringExtra("deviceMac");

            createNotificationChannel();

            // Create a notification for Foreground Service
            Notification notification = createNotification();

            // Display the notification
            startForeground(1337, notification);

            locationHelper = new LocationHelper(this, destinationLat, destinationLng, macAddress);
            locationHelper.startLocationUpdates();
        } else {
            // Handle case where extras are missing
        }
        return START_NOT_STICKY;
        //return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Location Channel";
            String description = "Channel for Location Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("LocationForegroundService", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Create a Notification for Foreground Service
    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "LocationForegroundService")
                .setContentTitle("Location Service")
                .setContentText("Pls don't shutdown.")
                .setSmallIcon(R.mipmap.ic_launcher); // Replace with your app's icon

        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationHelper != null) {
            locationHelper.stopLocationUpdates();
        }
    }
}

