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
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MainActivity";

    private BluetoothService bluetoothService;
    private boolean isBound = false;

    private SQLiteDatabaseHandler db;

    private EquipmentApiClient apiClient;

    // Location Manager
    private LocationManager locationManager;

    Device nearestDevice;

    public Device getNearestDevice() {
        return nearestDevice;
    }

    private CountDownLatch locationLatch;

    private static MainActivity instance;
    private static Intent serviceIntent;

    private static TextView logsTextView;

    private static boolean isBluetoothConnected;

    private GoogleMap googleMap;

    public static void setBluetoothConnected(boolean bluetoothConnected) {
        isBluetoothConnected = bluetoothConnected;
    }

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

    public static void appendToLogTextView(String textToAdd) {
        String currentText = logsTextView.getText().toString();
        String newText = currentText + "\n" + textToAdd;
        logsTextView.setText(newText);

        // Scroll to the bottom
        logsTextView.post(() -> {
            int scrollAmount = logsTextView.getLayout().getLineTop(logsTextView.getLineCount()) - logsTextView.getHeight();
            if (scrollAmount > 0)
                logsTextView.scrollTo(0, scrollAmount);
            else
                logsTextView.scrollTo(0, 0);
        });
    }

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

        logsTextView = findViewById(R.id.logTextView);
        logsTextView.setMovementMethod(new ScrollingMovementMethod());

        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        serviceIntent = new Intent(this, LocationForegroundService.class);

        db = new SQLiteDatabaseHandler(this);

        apiClient = new EquipmentApiClient();

        // Initialize Location Manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);
//        map.onCreate(savedInstanceState);


        // Create and start a new Thread to make the API call
        if (has_Internet()) {
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
                                        db.addDevice(new Device(equipment.getId(), equipment.getName(), equipment.getLatitude(), equipment.getLongitude(), equipment.getMac()));
                                    }
                                    appendToLogTextView("Adicionados dispositivos da API.");
                                } else {
                                    // Handle errors
                                    Log.e("API Error", "Failed to get equipment list");
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e("API Error", "Failed to get equipment: " + e.getMessage());
                        appendToLogTextView("API Error Failed to get equipment list.");
                    }
                }
            }).start();
        }

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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

                //Add log on TextView
                appendToLogTextView("Percurso Iniciado.");

                // Initialize the CountDownLatch with a count of 1
                locationLatch = new CountDownLatch(1);

                // Request a single location update
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        // Handle location update
                        Location androidPhoneLocation = new Location(location);

                        // Now that we have the location, find the nearest device
                        //TODO Work exception if no devices in db.
                        nearestDevice = findNearestDevice(db.getAllDevices(), androidPhoneLocation);

                        appendToLogTextView("O dispositivo mais proximo é: " + nearestDevice.getName());

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

                //stopService(serviceIntent);
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
                connectToBluetoothDevice(device);

            } else {
                Log.e(TAG, "Bluetooth adapter is not available or not enabled");
            }
        } else {
            Log.e(TAG, "Bluetooth service not bound");
        }
    }

    private void connectToBluetoothDevice(final BluetoothDevice device) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!bluetoothService.connectToDevice(device)) {
                    // If Bluetooth is not connected, attempt to connect and reschedule this Runnable
                    connectToBluetoothDevice(device); // Recursive call
                    appendToLogTextView("Tentei conectar com o parceiro.");
                } else {
                    // Bluetooth is connected, stop further executions
                    handler.removeCallbacksAndMessages(null);
                }
            }
        }, 1000);
    }

    private void checkLocation() {
        double destinationLat = nearestDevice.getLatitude();
        double destinationLng = nearestDevice.getLongitude();
        String macAddress = nearestDevice.getMac();

        serviceIntent.putExtra("destinationLat", destinationLat);
        serviceIntent.putExtra("destinationLng", destinationLng);
        serviceIntent.putExtra("deviceMac", macAddress);
        ContextCompat.startForegroundService(this, serviceIntent);
        appendToLogTextView("Iniciado o serviço de localização.");

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
        stopService(serviceIntent);
    }

    public void startSyncService() {
        try {
            db = new SQLiteDatabaseHandler(MainActivity.getInstance());
            apiClient = new EquipmentApiClient();
            ArrayList<Record> notSyncedRecords = db.getRecordNotSynced();
            if (!notSyncedRecords.isEmpty()) {
                Intent serviceIntent = new Intent(this, SyncService.class);
                startService(serviceIntent);
                appendToLogTextView("Sync service inicializado.");
            }
        } catch (Exception e) {
            Log.d(TAG, "startSyncService: " + e);
        }
    }

    private boolean has_Internet() {
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
    public void onMapReady(@NonNull GoogleMap googleMap) {
        LatLng targetCoordinates = new LatLng(39.826224, -7.751641);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(targetCoordinates);
        googleMap.moveCamera(cameraUpdate);
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(10.0f));
        ArrayList<Device> devices = db.getAllDevices();
        if(devices != null){
            if(!devices.isEmpty()){
                for (Device dev : devices) {
                    LatLng markerPosition = new LatLng(dev.getLatitude(), dev.getLongitude());
                    googleMap.addMarker(new MarkerOptions().position(markerPosition).title(dev.getName()));
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(true);

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            double currentLatitude = location.getLatitude();
            double currentLongitude = location.getLongitude();
            LatLng currentLatLng = new LatLng(currentLatitude, currentLongitude);
            CameraUpdate cameraUpdate_1 = CameraUpdateFactory.newLatLngZoom(currentLatLng, 19);
            googleMap.animateCamera(cameraUpdate_1);
        }

    }


}
