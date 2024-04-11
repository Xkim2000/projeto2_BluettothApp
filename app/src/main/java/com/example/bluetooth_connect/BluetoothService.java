package com.example.bluetooth_connect;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.PrivateKey;
import javax.crypto.KeyAgreement;

public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";

    private final IBinder binder = new BluetoothBinder();
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private UUID uuid;

    private int bufferSize = 0;

    private SQLiteDatabaseHandler db;
    KeyPair keyPair;

    public BluetoothService() {
        Log.e(TAG, "BluetoothBinder created");
    }
//////////////////////////////
    private PublicKey serverPublicKey; // Store server's public key

    // Generate RSA key pair
    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // Key size
        return keyPairGenerator.generateKeyPair();
    }

    // Send public key to server
    private void sendPublicKey() {
        try {
            keyPair = generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            byte[] publicKeyBytes = publicKey.getEncoded();
            // Send publicKeyBytes to server
            mOutputStream.write(publicKeyBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Receive server's public key with a timeout of 2 seconds
    public PublicKey receiveServerPublicKey() {
        PublicKey serverPublicKey = null;
        byte[] buffer = new byte[2048]; // key size
        int bytesCount;
        long startTime = System.currentTimeMillis();

        try {
            while (serverPublicKey == null && System.currentTimeMillis() - startTime < 2000) { // 2 seconds
                // Read from input stream
                bytesCount = mInputStream.read(buffer);
                if (bytesCount > 0) {
                    // Convert received bytes to PublicKey
                    byte[] decodedKeyBytes = decodePEM(new String(buffer, 0, bytesCount));
                    serverPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedKeyBytes));
                    //String publicKeyString = Base64.getEncoder().encodeToString(serverPublicKey.getEncoded());
                    //System.out.println(publicKeyString);
                }
            }
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Handle error
            e.printStackTrace();
        }

        if (serverPublicKey == null) {
            // Handle timeout here
            System.out.println("Timeout: Server's public key not received.");
        }

        return serverPublicKey;
    }

    private byte[] decodePEM(String pemKey) {
        String[] parts = pemKey.split("\n");
        StringBuilder pemBuilder = new StringBuilder();
        for (String part : parts) {
            if (!part.startsWith("-----")) {
                pemBuilder.append(part.trim());
            }
        }
        return Base64.getDecoder().decode(pemBuilder.toString());
    }

    // Generate shared secret
    private byte[] generateSharedSecret() {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(keyPair.getPrivate()); // Use app's private key
            keyAgreement.doPhase(serverPublicKey, true);
            return keyAgreement.generateSecret();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


///////////////////////////////
    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    }

    public class BluetoothBinder extends Binder implements CustomBinder {
        @Override
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // CustomBinder interface definition
    public interface CustomBinder {
        BluetoothService getService();
    }

    public void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Bluetooth device is null");
            return;
        }

        try {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    return;
                }
            }
            mBluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            try{
                mBluetoothSocket.connect();
            }catch (IOException connEx){
                closeConnection();
                return;
            }

            if (mBluetoothSocket != null && mBluetoothSocket.isConnected()) {
                // Socket is connected, now we can obtain our IO streams
                try {
                    //System.out.println("Estou conectado !!");
                    mInputStream = mBluetoothSocket.getInputStream();
                    mOutputStream = mBluetoothSocket.getOutputStream();

                    generateKeyPair();
                    sendPublicKey();
                    Log.d(TAG, "CHEGUEI");
                    receiveServerPublicKey();
                    generateSharedSecret();

                } catch (IOException e) {
                    // Handle error
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }

            Log.d(TAG, "Connected to device: " + device.getName());
            Log.d(TAG, "DESTRUI O SERVICO");
            Intent serviceIntent = new Intent(this, LocationForegroundService.class);
            stopService(serviceIntent);
            byte[] bytesToSend = uuid.toString().getBytes();
            sendData(bytesToSend);

        } catch (IOException e) {
            Log.e(TAG, "Connection failed: " + e.getMessage());
            closeConnection();
        }
    }


    public void sendData(byte[] bytes) {
        byte[] sharedSecret = generateSharedSecret();
        if (sharedSecret != null) {
            // Encrypt data using sharedSecret
            // Example: byte[] encryptedData = encrypt(bytes, sharedSecret);
            // Send encryptedData to server
            //TODO
            try {

                mOutputStream.write(bytes);

                // Wait for confirmation
                boolean confirmationReceived = receiveConfirmation();

                if (confirmationReceived) {
                    System.out.println("Message sent and confirmation received!");
                    //showToast("Message sent and confirmation received!");
                    sendReadyForDataMessage();
                    receiveBufferSize();
                    if(bufferSize != 0){
                        if(receiveDataJSON()){
                            MainActivity.getInstance().startSyncService();
                        }
                    }

                } else {
                    System.out.println("Confirmation not received. Message may not have been delivered.");
                    //showToast("Confirmation not received. Message may not have been delivered.");
                    closeConnection();
                }

            } catch (IOException e) {
                // Handle error
            }
        }
    }

    private void sendReadyForDataMessage() throws IOException {
        String ready = "Ready for data";
        mOutputStream.write(ready.getBytes());
    }

    public boolean receiveConfirmation() {
        byte[] buffer = new byte[1024];
        int bytesCount;
        boolean confirmationReceived = false;
        long startTime = System.currentTimeMillis();

        try {
            while (!confirmationReceived && System.currentTimeMillis() - startTime < 2000) { // 2 seconds
                // Read from input stream
                bytesCount = mInputStream.read(buffer);
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

    public void receiveBufferSize() {
        byte[] buffer = new byte[1024];
        int bytesCount;
        long startTime = System.currentTimeMillis();

        try {
            while ( System.currentTimeMillis() - startTime < 2000) { // 2 seconds
                // Read from input stream
                bytesCount = mInputStream.read(buffer);
                if (bytesCount > 0) {
                    String receivedData = new String(buffer, 0, bytesCount);
                    // Check for confirmation message
                    bufferSize = Integer.parseInt(receivedData);
                    break;
                }
            }
        } catch (IOException e) {
            // Handle error
        }
    }

    public boolean receiveDataJSON() {
        byte[] buffer = new byte[bufferSize];
        int bytesCount;
        boolean dataReceived = false;
        long startTime = System.currentTimeMillis();

        try {
            while (!dataReceived && System.currentTimeMillis() - startTime < 2000) { // 2 seconds
                // Read from input stream
                bytesCount = mInputStream.read(buffer);
                if (bytesCount > 0) {
                    String receivedData = new String(buffer, 0, bytesCount);

                    // Parse the received JSON data
                    try {
                        JSONArray jsonArray = new JSONArray(receivedData);
                        db = new SQLiteDatabaseHandler(this);
                        MainActivity ma = MainActivity.getInstance();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);

                            // Extract data from JSON object
                            String classData = jsonObject.getString("class");
                            String timestamp = jsonObject.getString("timestamp");

                            // Now you can use the extracted data as needed
                            System.out.println("Class: " + classData + ", Timestamp: " + timestamp);

                            db.addRecord(new Record(classData,timestamp,ma.getNearestDevice().getId(),false));
                        }

                        // Set dataReceived to true since we received and processed data
                        dataReceived = true;

                    } catch (JSONException e) {
                        e.printStackTrace();
                        // Handle JSON parsing error
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle error reading from input stream
        }

        if (!dataReceived) {
            // Handle timeout here
            System.out.println("Timeout: Data not received.");
            //showToast("Timeout: Confirmation not received.");
        }

        return dataReceived;
    }


    public void receiveData() {
        if (mInputStream == null) {
            Log.e(TAG, "Input stream is not initialized");
            return;
        }

        byte[] buffer = new byte[1024];
        int bytes;

        while (true) {
            try {
                bytes = mInputStream.read(buffer);
                if (bytes > 0) {
                    String receivedData = new String(buffer, 0, bytes);
                    Log.d(TAG, "Received data: " + receivedData);
                    // Handle received data as needed
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading from input stream: " + e.getMessage());
                break;
            }
        }
    }

    public void closeConnection() {
        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
            if (mOutputStream != null) {
                mOutputStream.close();
            }
            if (mBluetoothSocket != null) {
                mBluetoothSocket.close();
            }
            Log.d(TAG, "Bluetooth connection closed");
        } catch (IOException e) {
            Log.e(TAG, "Error closing Bluetooth connection: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeConnection();
    }
}