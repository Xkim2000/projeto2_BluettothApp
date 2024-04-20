package com.example.bluetooth_connect;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncService extends Service {
    private static final String TAG = "SyncService";
    private static final long LOG_INTERVAL = 15 * 1000; // 15 seconds
    private static final int NOTIFICATION_ID = 1234;
    private static final String NOTIFICATION_CHANNEL_ID = "SyncChannel";

    private SQLiteDatabaseHandler db;
    private EquipmentApiClient apiClient;

    private PowerManager.WakeLock wakeLock;
    private Handler handler;
    private Runnable syncRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Sync Service created");

        // Create a notification channel (required for Android 8.0 and higher)
        createNotificationChannel();

        // Create a notification for Foreground Service
        Notification notification = createNotification();

        // Display the notification
        startForeground(NOTIFICATION_ID, notification);

        // Acquire wake lock to keep the CPU running
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "SyncService::WakeLock");
        wakeLock.acquire();

        // Check for permissions if running on Android 9 (Pie) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Initialize Handler and Runnable for periodic syncing
        handler = new Handler();
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                syncData();
                handler.postDelayed(this, LOG_INTERVAL);
            }
        };
        handler.postDelayed(syncRunnable, LOG_INTERVAL);
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

        // Stop the Foreground Service and remove the notification
        stopForeground(true);

        // Release the wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Stop the periodic syncing
        if (handler != null && syncRunnable != null) {
            handler.removeCallbacks(syncRunnable);
        }
    }


    private void syncData() {
        // Implement your data sync logic here
        // This could involve network calls, updating the SQLite database, etc.
        if (hasInternet()) {
            db = new SQLiteDatabaseHandler(MainActivity.getInstance());
            apiClient = new EquipmentApiClient();
            ArrayList<Record> notSyncedRecords = db.getRecordNotSynced();

            if (notSyncedRecords != null) {
                Log.d(TAG, "Device has Internet !");
                Log.d(TAG, "Syncing data...");


                Call<Integer> call = apiClient.insertUserData(notSyncedRecords);

                call.enqueue(new Callback<Integer>() {
                    @Override
                    public void onResponse(Call<Integer> call, Response<Integer> response) {

                        if (response.isSuccessful() && (response.body().equals(notSyncedRecords.size()))) {
                            // Código de resposta 200-299 indica sucesso
                            //Log.d(TAG, "Todos os Registros adicionados com sucesso!");

                            for (Record record: notSyncedRecords){
                                record.setIsSynced(true);
                                db.updateRecordToSynced(record);
                            }
                            db.deleteAlreadySynced();
                            MainActivity.appendToLogTextView("Dados bem sincronizados com a base de dados central.");
                            stopSelf();

                        } else {
                            // Código de resposta diferente de 200-299 indica falha
                            Log.d(TAG, "Falha ao adicionar registros. Código de resposta: " + response.code());
                            MainActivity.appendToLogTextView("Falha ao adicionar registros. Código de resposta: " + response.code());
                        }
                    }
                    @Override
                    public void onFailure(Call<Integer> call, Throwable t) {
                        // Caso ocorra uma exceção durante a chamada
                        //System.out.println("Erro ao adicionar registros: " + t.getMessage());
                        Log.d(TAG, "Erro ao adicionar registros: " + t.getMessage());
                        MainActivity.appendToLogTextView("Erro ao adicionar registros: " + t.getMessage());
                    }
                });

            }
        } else {
            Log.d(TAG, "NO INTERNET!");
        }

    }

    private boolean hasInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = connectivityManager.getActiveNetwork();

        if (activeNetwork != null) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                // Device has internet connection
                return true;
            } else {
                // Device does not have internet connection
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Create a Notification Channel (required for Android 8.0 and higher)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Sync Channel";
            String description = "Channel for Sync Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Create a Notification for Foreground Service
    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Sync Service")
                .setContentText("This service is sending data to the cloud. Pls don't shutdown.")
                .setSmallIcon(R.mipmap.ic_launcher); // Replace with your app's icon

        return builder.build();
    }
}
