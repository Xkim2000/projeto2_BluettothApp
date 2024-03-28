package com.example.bluetooth_connect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.example.bluetooth_connect.BluetoothService.BluetoothBinder;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private BluetoothService bluetoothService;
    private boolean isBound = false;

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

//        connectButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (bluetoothService != null && isBound) {
//                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//                    if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
//                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice("D8:3A:DD:36:E5:30");
//                        bluetoothService.connectToDevice(device);
//                    } else {
//                        Log.e(TAG, "Bluetooth adapter is not available or not enabled");
//                    }
//                } else {
//                    Log.e(TAG, "Bluetooth service not bound");
//                }
//            }
//        });

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
}
