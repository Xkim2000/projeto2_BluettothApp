package com.example.bluetooth_connect;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity_2 extends AppCompatActivity {

    private Button initiateConnectionButton;

    private BluetoothAdapter mBluetoothAdapter;

    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    showToast("Bluetooth Enabled");
                    // Since Bluetooth is enabled, proceed to connect directly to the device
                    connectToDevice();
                }
            }
        }

    };

    private void connectToDevice() {
        // Get the Bluetooth adapter
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check if Bluetooth adapter is null or not enabled
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            showToast("Bluetooth adapter is not available or not enabled");
            return;
        }

        // Get the Bluetooth device by its known MAC address
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("D8:3A:DD:36:E5:30");

        // Check if device is null
        if (device == null) {
            showToast("Device not found. Make sure it is in range and Bluetooth is enabled.");
            return;
        }

        // Check for Bluetooth connect permission
        if (ContextCompat.checkSelfPermission(MainActivity_2.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(MainActivity_2.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        // Cancel any ongoing discovery
        mBluetoothAdapter.cancelDiscovery();

        // Connect to the Bluetooth device
        initializeSocket(device, uuid);
        connectSocket();

        // Send data or perform any other necessary operations
        byte[] bytesToSend = uuid.toString().getBytes();
        sendData(bytesToSend);

        showToast("Connecting to device " + device.getName() + " (" + device.getAddress() + ")");
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check and request GPS if not enabled
        checkAndRequestGPS();

        // Initialize Bluetooth components
        initializeBluetooth();

//        // Connect directly to the known Bluetooth device
//        connectToDeviceDirectly();
    }

    private void checkAndRequestGPS() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGpsEnabled) {
            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 1);
        }
    }

    private void initializeBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        initiateConnectionButton = findViewById(R.id.buttonStart);
        Button stopConnectionButton = findViewById(R.id.buttonStop);

        ProgressDialog mProgressDlg = new ProgressDialog(this);
        mProgressDlg.setMessage("Scanning...");
        mProgressDlg.setCancelable(false);
        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> {
            dialog.dismiss();

            if (ContextCompat.checkSelfPermission(MainActivity_2.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(MainActivity_2.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                    return;
                }
            }
            mBluetoothAdapter.cancelDiscovery();
        });

        initiateConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                showToast("Start Discovering!");
                if (ContextCompat.checkSelfPermission(MainActivity_2.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(MainActivity_2.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                        return;
                    }
                }
                connectToDeviceDirectly();
                //mBluetoothAdapter.startDiscovery();
            }
        });

        stopConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //_socket.close();
                closeConnection();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);
    }

    private void connectToDeviceDirectly() {
        // Connect directly to the known Bluetooth device
        connectToDevice();
    }


    private void showEnabled() {
        /*mStatusTv.setText("Bluetooth is On");
        mStatusTv.setTextColor(Color.BLUE);*/

        /*mActivateBtn.setText("Disable");
        mActivateBtn.setEnabled(true);*/

        /*mPairedBtn.setEnabled(true);*/
        initiateConnectionButton.setEnabled(true);
    }



    private BluetoothSocket _socket;
    private InputStream _inStream;
    private OutputStream _outStream;
    //TODO Verify if the uuid is correct
    public void initializeSocket(BluetoothDevice device, UUID uuid) {

        try {
            if (ContextCompat.checkSelfPermission(MainActivity_2.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
            {
                //System.out.println("NO PERMISSIONS ON SCAN");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    ActivityCompat.requestPermissions(MainActivity_2.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                    return;
                }
            }
            _socket = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            // Handle error
        }
    }

    public void connectSocket() {
        try {
            if (ContextCompat.checkSelfPermission(MainActivity_2.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
            {
                //System.out.println("NO PERMISSIONS ON SCAN");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    ActivityCompat.requestPermissions(MainActivity_2.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                    return;
                }
            }
            _socket.connect();
        } catch (IOException connEx) {
            closeConnection();
            //_socket.close();
        }

        if (_socket != null && _socket.isConnected()) {
            // Socket is connected, now we can obtain our IO streams
            try {
                //System.out.println("Estou conectado !!");
                _inStream = _socket.getInputStream();
                _outStream = _socket.getOutputStream();
            } catch (IOException e) {
                // Handle error
            }
        }
    }

    public void receiveData() {
        byte[] buffer = new byte[1024];  // Buffer for our data
        int bytesCount; // Amount of read bytes

        while (true) {
            try {
                // Reading data from input stream
                bytesCount = _inStream.read(buffer);
                if (buffer != null && bytesCount > 0) {
                    // Parse received bytes
                    String receivedData = new String(buffer, 0, bytesCount);
                    System.out.println("Received data: " + receivedData);
                }
            } catch (IOException e) {
                // Handle error
            }
        }
    }

    public boolean receiveConfirmation() {
        byte[] buffer = new byte[1024];
        int bytesCount;
        boolean confirmationReceived = false;
        long startTime = System.currentTimeMillis();

        try {
            while (!confirmationReceived && System.currentTimeMillis() - startTime < 2000) { // 2 seconds
                // Read from input stream
                bytesCount = _inStream.read(buffer);
                if (bytesCount > 0) {
                    String receivedData = new String(buffer, 0, bytesCount);
                    // Check for confirmation message
                    if (receivedData.equals("ConfirmationMessage")) {
                        confirmationReceived = true;
                    }
                }
            }
        } catch (IOException e) {
            // Handle error
        }

        if (!confirmationReceived) {
            // Handle timeout here
            System.out.println("Timeout: Confirmation not received.");
            //showToast("Timeout: Confirmation not received.");
        }

        return confirmationReceived;
    }




    public void sendData(byte[] bytes) {
        try {
            _outStream.write(bytes);

            // Wait for confirmation
            boolean confirmationReceived = receiveConfirmation();

            if (confirmationReceived) {
                System.out.println("Message sent and confirmation received!");
                //showToast("Message sent and confirmation received!");
                sendReadyForDataMessage();
            } else {
                System.out.println("Confirmation not received. Message may not have been delivered.");
                //showToast("Confirmation not received. Message may not have been delivered.");
                closeConnection();
            }

        } catch (IOException e) {
            // Handle error
        }
    }

    private void sendReadyForDataMessage() throws IOException {
        String ready = "Ready for data";
        _outStream.write(ready.getBytes());
    }



    /*public void sendData(byte[] bytes) {
        try {
            _outStream.write(bytes);
        } catch (IOException e) {
            // Handle error
        }
    }*/


    public void closeConnection() {
        try {
            if (_inStream != null) {
                _inStream.close();
            }
            if (_outStream != null) {
                _outStream.close();
            }
            if (_socket != null) {
                _socket.close();
            }
        } catch (IOException e) {
            System.out.println("No connection OPEN !!!");
        }
    }
}