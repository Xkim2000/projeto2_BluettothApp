package com.example.bluetooth_connect;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private BluetoothService bluetoothService;
    private boolean isBound = false;

    private SQLiteDatabaseHandler db;

    private EquipmentApiClient apiClient;

    // Location Manager
    private LocationManager locationManager;

    Device nearestDevice;

    private CountDownLatch locationLatch;

    private static MainActivity instance;

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

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        instance = this;

        Button connectButton = findViewById(R.id.buttonStart);
        Button disconnectButton = findViewById(R.id.buttonStop);

        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);


        db = new SQLiteDatabaseHandler(this);

        //Device deviceTest = new Device("PRN-d83add36e52c", "Proença-a-Nova-3", 39.781908022317424, -7.8138558910675275);
        //db.addDevice(deviceTest);

        //db.getAllDevices();

        Record record1 = new Record(1, "111", (LocalDateTime.now()).toString(), "PRN-1");
        Record record2 = new Record(2, "222", (LocalDateTime.now()).toString(), "PRN-1");
        Record record3 = new Record(3, "3333", (LocalDateTime.now()).toString(), "PRN-1");
        Record record4 = new Record(4, "4444", (LocalDateTime.now()).toString(), "PRN-1");

        ArrayList<Record> records = new ArrayList<>();
        records.add(record1);
        records.add(record2);
        records.add(record3);
        records.add(record4);

        for (Record record : records) {
            db.addRecord(record);
        }


        apiClient = new EquipmentApiClient();

        // Initialize Location Manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

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

        //createNotificationChannel();

        //TODO Remove startSyncService from the OnCreate
        //SYNC Service initalization
        startSyncService();

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //TODO replace the code below:

                // Check for permissions
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(MainActivity.this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION},
                            1);
                    return;
                }

                // Initialize the CountDownLatch with a count of 1
                locationLatch = new CountDownLatch(1);

                // Request a single location update
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        // Handle location update
                        Location androidPhoneLocation = new Location(location);

                        // Now that we have the location, find the nearest device
                        nearestDevice = findNearestDevice(db.getAllDevices(), androidPhoneLocation);

                        // Signal the CountDownLatch
                        locationLatch.countDown();
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }
                }, null);

                // Create a separate thread to wait for the latch and call checkLocation()
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            locationLatch.await(); // Wait until location is received
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    checkLocation();
                                }
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();


                //makeConnection();
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

    protected void makeConnection() {
        if (bluetoothService != null && isBound) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(nearestDevice.getMac());

                //TODO exception treatment for the connection not successfully

                // Start a 2-second delay before connecting
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bluetoothService.connectToDevice(device);

                        //SYNC Service initalization
                        startSyncService();
                    }
                }, 2000); // 2000 milliseconds = 2 seconds

            } else {
                Log.e(TAG, "Bluetooth adapter is not available or not enabled");
            }
        } else {
            Log.e(TAG, "Bluetooth service not bound");
        }
    }


    private void checkLocation() {
        double destinationLat = nearestDevice.getLatitude();
        double destinationLng = nearestDevice.getLongitude();
        String macAddress = nearestDevice.getMac();
        Intent serviceIntent = new Intent(MainActivity.this, LocationForegroundService.class);
        serviceIntent.putExtra("destinationLat", destinationLat);
        serviceIntent.putExtra("destinationLng", destinationLng);
        serviceIntent.putExtra("deviceMac", macAddress);
        ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
    }

    // Method to find the nearest device
    private Device findNearestDevice(List<Device> devices, Location location) {
        if (devices.isEmpty()) {
            throw new IllegalArgumentException("Device list is empty");
        }

        double minDistance = Double.MAX_VALUE;
        Device nearestDevice = null;

        for (Device device : devices) {
            float[] results = new float[1];
            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                    device.getLatitude(), device.getLongitude(), results);
            double distance = results[0];

            if (distance < minDistance) {
                minDistance = distance;
                nearestDevice = device;
            }
        }

        return nearestDevice;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        Intent serviceIntent = new Intent(this, LocationForegroundService.class);
        stopService(serviceIntent);
    }

    private void startSyncService() {
        try {
            Intent serviceIntent = new Intent(this, SyncService.class);
            startService(serviceIntent);
        } catch (Exception e) {
            Log.d(TAG, "startSyncService: " + e);
        }
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


}
