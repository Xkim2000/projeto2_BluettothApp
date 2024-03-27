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
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button mScanBtnPresencas;

    private ProgressDialog mProgressDlg;
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    private BluetoothAdapter mBluetoothAdapter;

    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGpsEnabled) {
            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 1);
        }

        mScanBtnPresencas = (Button) findViewById(R.id.buttonSearch);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mProgressDlg = new ProgressDialog(this);

        mProgressDlg.setMessage("Scanning...");
        mProgressDlg.setCancelable(false);
        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
                {
                    //System.out.println("NO PERMISSIONS ON SCAN");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                        return;
                    }
                }
                mBluetoothAdapter.cancelDiscovery();
            }
        });

        mScanBtnPresencas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                showToast("Start Discovering!");
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
                {
                    //System.out.println("NO PERMISSIONS ON SCAN");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                        return;
                    }
                }


                mBluetoothAdapter.startDiscovery();
            }
        });

        IntentFilter filter = new IntentFilter();


        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);


    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {
                    showToast("Enabled");

                    showEnabled();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                mDeviceList = new ArrayList<BluetoothDevice>();

                mProgressDlg.show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mProgressDlg.dismiss();

                /*Intent newIntent = new Intent(AdicionarPresencaBlt.this, DeviceListActivityPresencas.class);

                newIntent.putParcelableArrayListExtra("device.list", mDeviceList);

                startActivity(newIntent);*/
                System.out.println(mDeviceList);
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                mDeviceList.add(device);
                if(device.getAddress().equals("D8:3A:DD:36:E5:30")) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
                    {
                        //System.out.println("NO PERMISSIONS ON SCAN");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                            return;
                        }
                    }
                    mBluetoothAdapter.cancelDiscovery();
                    mProgressDlg.dismiss();
                    BluetoothDevice deviceToConnect = mBluetoothAdapter.getRemoteDevice("D8:3A:DD:36:E5:30");
                    initializeSocket(deviceToConnect, uuid);
                    connectSocket();

                    String message = "Hello, from Android Device!";
                    byte[] bytesToSend = uuid.toString().getBytes();
                    sendData(bytesToSend);

                }
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
                {
                    //System.out.println("NO PERMISSIONS ON GETNAME OF DEVICE");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                        return;
                    }
                }

                showToast("Found device " + device.getName());
            }
        }
    };

    private void showEnabled() {
        /*mStatusTv.setText("Bluetooth is On");
        mStatusTv.setTextColor(Color.BLUE);*/

        /*mActivateBtn.setText("Disable");
        mActivateBtn.setEnabled(true);*/

        /*mPairedBtn.setEnabled(true);*/
        mScanBtnPresencas.setEnabled(true);
    }



    private BluetoothSocket _socket;
    private InputStream _inStream;
    private OutputStream _outStream;
    //TODO Verify if the uuid is correct
    public void initializeSocket(BluetoothDevice device, UUID uuid) {

        try {
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
            {
                //System.out.println("NO PERMISSIONS ON SCAN");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
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
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED)
            {
                //System.out.println("NO PERMISSIONS ON SCAN");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                    return;
                }
            }
            _socket.connect();
        } catch (IOException connEx) {
            try {
                _socket.close();
            } catch (IOException closeException) {
                // Handle error
            }
        }

        if (_socket != null && _socket.isConnected()) {
            // Socket is connected, now we can obtain our IO streams
            try {
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

    public void sendData(byte[] bytes) {
        try {
            _outStream.write(bytes);
        } catch (IOException e) {
            // Handle error
        }
    }

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
            // Handle error
        }
    }
}