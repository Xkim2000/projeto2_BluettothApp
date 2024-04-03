package com.example.bluetooth_connect;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private BluetoothService bluetoothService;
    private boolean isBound = false;

    private SQLiteDatabaseHandler db;

    private EquipmentApiClient apiClient;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // Check if the IBinder is an instance of CustomBinder
            if (iBinder instanceof BluetoothService.CustomBinder) {
                BluetoothService.CustomBinder binder = (BluetoothService.CustomBinder) iBinder;
                bluetoothService = binder.getService();
                isBound = true;
            } else {
                Log.e(TAG, "Unexpected IBinder class: " + iBinder.getClass().getName());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button connectButton = findViewById(R.id.buttonStart);
        Button disconnectButton = findViewById(R.id.buttonStop);

        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);


        db = new SQLiteDatabaseHandler(this);

        //Device deviceTest = new Device("PRN-d83add36e52c", "Proença-a-Nova-3", 39.781908022317424, -7.8138558910675275);
        //db.addDevice(deviceTest);

        //db.getAllDevices();

        apiClient = new EquipmentApiClient();

        // Create and start a new Thread to make the API call
        if(has_Internet()){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Make the API call to get all equipment
                        List<Device> equipmentList = apiClient.getAllEquipment().execute().body();

                        // Handle the list of equipment on the main thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (equipmentList != null) {
                                    // Handle the list of equipment as needed
                                    for (Device equipment : equipmentList) {
                                        //Log.d("Equipment", "ID: " + equipment.getId() + ", Name: " + equipment.getName());
                                        // Do something with each equipment item
                                        db.addDevice(new Device(equipment.getId(),equipment.getName(), equipment.getLatitude(), equipment.getLongitude(), equipment.getMac()));
                                    }
                                } else {
                                    // Handle errors
                                    Log.e("API Error", "Failed to get equipment list");
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e("API Error", "Failed to get equipment: " + e.getMessage());
                    }
                }
            }).start();
        }

/*
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothService != null && isBound) {
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice("D8:3A:DD:36:E5:30");
                        bluetoothService.connectToDevice(device);
                    } else {
                        Log.e(TAG, "Bluetooth adapter is not available or not enabled");
                    }
                } else {
                    Log.e(TAG, "Bluetooth service not bound");
                }
            }
        });
*/

        createNotificationChannel();

        //SYNC Service initalization
        startSyncService();

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothService != null && isBound) {
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice("D8:3A:DD:36:E5:30");

                        //TODO exception treatment for the connection not successfully

                        // Start a 2-second delay before connecting
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                bluetoothService.connectToDevice(device);
                            }
                        }, 2000); // 2000 milliseconds = 2 seconds

                    } else {
                        Log.e(TAG, "Bluetooth adapter is not available or not enabled");
                    }
                } else {
                    Log.e(TAG, "Bluetooth service not bound");
                }
            }
        });


        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothService != null && isBound) {
                    bluetoothService.closeConnection();
                } else {
                    Log.e(TAG, "Bluetooth service not bound");
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void startSyncService() {
        Intent serviceIntent = new Intent(this, SyncService.class);
        startService(serviceIntent);
    }

    private boolean has_Internet(){
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Sync Service Channel";
            String description = "Sync Service Channel Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("ChannelID", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
