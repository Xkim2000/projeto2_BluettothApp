package com.example.bluetooth_connect;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

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
            MainActivity.appendToLogTextView("Public key ENVIADA.");
        } catch (NoSuchAlgorithmException e) {
            MainActivity.appendToLogTextView("Public key NÃO ENVIADA.");
            e.printStackTrace();
        } catch (IOException e) {
            MainActivity.appendToLogTextView("Public key NÃO ENVIADA.");
            throw new RuntimeException(e);
        }
    }

    // Receive server's public key with a timeout of 2 seconds
    public void receiveServerPublicKey() {
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
                    String publicKeyString = Base64.getEncoder().encodeToString(serverPublicKey.getEncoded());
                    //System.out.println(publicKeyString);
                    MainActivity.appendToLogTextView("Public key do parceiro RECEBIDA.");
                }
            }
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            MainActivity.appendToLogTextView("Public key do parceiro NÃO RECEBIDA.");
            // Handle error
            e.printStackTrace();
        }

        if (serverPublicKey == null) {
            // Handle timeout here
            System.out.println("Timeout: Server's public key not received.");
        }

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
///////////////////////////////

    public static SecretKey generateAESKey() {
        try {
            // Create a KeyGenerator object for AES
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");

            // Initialize the KeyGenerator with the specified key size
            keyGen.init(256);

            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Send the AES key encrypted with RSA public key
    public byte[] getEncryptedAESKey(SecretKey aesKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        String encodedKey = Base64.getEncoder().encodeToString(aesKey.getEncoded());
//        System.out.println(encodedKey);
//        System.out.println(encodedKey.length());

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
        byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());

        //System.out.println(Arrays.toString(encryptedKey));
        String base64Key = Base64.getEncoder().encodeToString(encryptedKey);
        // Print the Base64 encoded string
//        System.out.println("Encrypted Key (Base64):");
//        System.out.println(base64Key);

        // Send encryptedKey to server
        //mOutputStream.write(encryptedKey);
        return encryptedKey;
    }

    public void sendDataEncryptedWithAES(String data) {
        try {
            SecretKey aesKey = generateAESKey();
            byte[] encryptedKey = getEncryptedAESKey(aesKey);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            byte[] encryptedAESAndData = mergeArrays(encryptedKey, encryptedData);
            // Send encryptedData to server
            mOutputStream.write(encryptedAESAndData);
        } catch (Exception e) {
            MainActivity.appendToLogTextView("Dados encriptados NÃO enviados.");
            e.printStackTrace();
        }
    }

    public static byte[] mergeArrays(byte[] array1, byte[] array2) {
        byte[] merged = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, merged, 0, array1.length);
        System.arraycopy(array2, 0, merged, array1.length, array2.length);
        return merged;
    }

    // Método para receber os dados criptografados do cliente
    public byte[] receiveDataEncryptedWithAES() {
        try {
            byte[] encryptedData;
            if (bufferSize == 0){
                encryptedData = new byte[2048];
            }else{
                int nextDivisibleBy32 = ((bufferSize + 31) / 32) * 32;

                encryptedData = new byte[nextDivisibleBy32];
            }
            // Tamanho máximo dos dados criptografados
            int bytesRead = mInputStream.read(encryptedData);
            if (bytesRead == -1) {
                // Não há dados para ler
                return null;
            }

            // Separar a chave AES e os dados criptografados
            byte[] encryptedAesKey = Arrays.copyOfRange(encryptedData, 0, 256);
            byte[] encryptedDataOnly = Arrays.copyOfRange(encryptedData, 256, bytesRead);

            // Descriptografar a chave AES usando a chave privada RSA
            // Criar uma instância da chave AES
            SecretKey aesKey = decryptAesKey(encryptedAesKey);
            //Log.d(TAG, "aesEncodedKey: " + Base64.getEncoder().encodeToString(aesKey.getEncoded()));


            // Descriptografar os dados usando a chave AES
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            byte[] decryptedData = cipher.doFinal(encryptedDataOnly);
            // Exibir os dados descriptografados
            //System.out.println("Decrypted data: " + new String(decryptedData, StandardCharsets.UTF_8));

            MainActivity.appendToLogTextView("Dados foram recebidos e desencriptados.");
            return decryptedData;

        } catch (Exception e) {
            MainActivity.appendToLogTextView("Dados NÃO foram recebidos.");
            e.printStackTrace();
        }
        return null;
    }

    private SecretKeySpec decryptAesKey(byte[] encryptedAesKey) {
        try {
            // Criar um objeto Cipher para descriptografar com RSA
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());

            // Descriptografar a chave AES
            byte[] aesKeyBytes = cipher.doFinal(encryptedAesKey);
            MainActivity.appendToLogTextView("Chave AES recebida foi desencriptada");
            return new SecretKeySpec(aesKeyBytes, "AES");
        } catch (Exception e) {
            MainActivity.appendToLogTextView("Chave AES recebida NÃO foi desencriptada");
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
                MainActivity.appendToLogTextView("Bluetooth conectado com o dispositivo.");
                // Socket is connected, now we can obtain our IO streams
                try {
                    //System.out.println("Estou conectado !!");
                    mInputStream = mBluetoothSocket.getInputStream();
                    mOutputStream = mBluetoothSocket.getOutputStream();

                    //Public keys Exchange
                    //generateKeyPair();
                    sendPublicKey();
                    receiveServerPublicKey();
                    ///////////////////////

                    //Data Exchange
                    sendDataEncryptedWithAES(uuid.toString());
                    MainActivity.appendToLogTextView("UUID encriptado enviado.");
                    //Receive confirmation message
                    receiveDataEncryptedWithAES();

                    // Send ready for data encrypted with AES
                    sendDataEncryptedWithAES("Ready for data");
                    MainActivity.appendToLogTextView("Ready for data ENVIADO");

                    //Receive buffer size
                    receiveBufferSize();

                    //Receive json data and start sync service
                    String stringDados = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
                    //System.out.println(stringDados);
                    sendDataEncryptedWithAES(Integer.toString(countJSONData(stringDados)));

//                    String dataConfirmation;
//                    do {
//                        dataConfirmation = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
//                        if(dataConfirmation.equals("Num registos incorreto")){
//                            stringDados = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
//                            //System.out.println(stringDados);
//                            sendDataEncryptedWithAES(Integer.toString(countJSONData(stringDados)));
//                        }
//                    }while (!dataConfirmation.equals("Conexao terminada"));

                    String dataConfirmation = "";
                    while (!dataConfirmation.equals("Conexao terminada")){
                        dataConfirmation = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
                        if(dataConfirmation.equals("Num registos incorreto")){
                            stringDados = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
                            //System.out.println(stringDados);
                            sendDataEncryptedWithAES(Integer.toString(countJSONData(stringDados)));
                        }else{
                            if(processDataJSON(stringDados)){
                                MainActivity.getInstance().startSyncService();
                            }
                        }
                    }
                    closeConnection();
                    //checkExchangeDataSuccessfuly();
                    ///////////////////////

                } catch (IOException e) {
                    // Handle error
                }
            }

            Log.d(TAG, "Connected to device: " + device.getName());
            Log.d(TAG, "DESTRUI O SERVICO");
            Intent serviceIntent = new Intent(this, LocationForegroundService.class);
            stopService(serviceIntent);
//            byte[] bytesToSend = uuid.toString().getBytes();
//            sendData(bytesToSend);

        } catch (IOException e) {
            Log.e(TAG, "Connection failed: " + e.getMessage());
            closeConnection();
        }
    }


    public void receiveBufferSize() {
        String bufferSizeString = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
        bufferSize = Integer.parseInt(bufferSizeString);
        MainActivity.appendToLogTextView("Buffersize recebido. Tamanho: " + bufferSize);
    }

    public void checkExchangeDataSuccessfuly() {
        String closeConectionString = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
        if(closeConectionString.equals("Conexao terminada")){
            closeConnection();
        }
        MainActivity.appendToLogTextView("Conexão terminada. Troca de dados bem sucedida");
    }

    public int countJSONData (String receivedData){
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(receivedData);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        int count = jsonArray.length();
        return  count;
    }

    public boolean processDataJSON(String receivedData) {
        boolean dataReceived = false;

        // Parse the received JSON data
        try {
            JSONArray jsonArray = new JSONArray(receivedData);
            jsonArray.length();
            db = new SQLiteDatabaseHandler(this);
            MainActivity ma = MainActivity.getInstance();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                // Extract data from JSON object
                String classData = jsonObject.getString("class");
                String timestamp = jsonObject.getString("timestamp");

                // Now you can use the extracted data as needed
                //System.out.println("Class: " + classData + ", Timestamp: " + timestamp);

                db.addRecord(new Record(classData,timestamp,ma.getNearestDevice().getId(),false));
            }

            // Set dataReceived to true since we received and processed data
            dataReceived = true;
            bufferSize = 0;
            MainActivity.appendToLogTextView("Dados inseridos na BD local.");

        } catch (JSONException e) {
            e.printStackTrace();
            // Handle JSON parsing error
        }

        if (!dataReceived) {
            // Handle timeout here
            System.out.println("Timeout: Data not received.");
            //showToast("Timeout: Confirmation not received.");
        }

        return dataReceived;
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