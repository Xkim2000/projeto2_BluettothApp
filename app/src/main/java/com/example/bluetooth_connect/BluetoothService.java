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
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import okio.Timeout;

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
            //MainActivity.appendToLogTextView("Public key ENVIADA.");
            MainActivity.appendToLogTextView("Public key SENT.");
        } catch (NoSuchAlgorithmException e) {
            //MainActivity.appendToLogTextView("Public key NÃO ENVIADA.");
            MainActivity.appendToLogTextView("Public key NOT SENT.");
            e.printStackTrace();
        } catch (IOException e) {
            //MainActivity.appendToLogTextView("Public key NÃO ENVIADA.");
            MainActivity.appendToLogTextView("Public key NOT SENT.");
            throw new RuntimeException(e);
        }
    }

    // Receive server's public key with a timeout of 2 seconds
//    public void receiveServerPublicKey() {
//        byte[] buffer = new byte[2048]; // key size
//        int bytesCount;
//        long startTime = System.currentTimeMillis();
//
//        try {
//            while (serverPublicKey == null && System.currentTimeMillis() - startTime < 2000) { // 2 seconds
//                // Read from input stream
//                bytesCount = mInputStream.read(buffer);
//                if (bytesCount > 0) {
//                    // Convert received bytes to PublicKey
//                    byte[] decodedKeyBytes = decodePEM(new String(buffer, 0, bytesCount));
//                    serverPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedKeyBytes));
//                    String publicKeyString = Base64.getEncoder().encodeToString(serverPublicKey.getEncoded());
//                    //System.out.println(publicKeyString);
//                    MainActivity.appendToLogTextView("Public key do parceiro RECEBIDA.");
//                }
//            }
//        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
//            MainActivity.appendToLogTextView("Public key do parceiro NÃO RECEBIDA.");
//            // Handle error
//            e.printStackTrace();
//        }
//
//        if (serverPublicKey == null) {
//            // Handle timeout here
//            System.out.println("Timeout: Server's public key not received.");
//        }
//
//    }
     //Receive server's public key with a timeout of 2 seconds
    public void receiveServerPublicKey() throws Exception{
        byte[] buffer = new byte[2048]; // key size
        int bytesCount;

        // Create a flag to indicate if data is read successfully
        final boolean[] dataRead = {false};

        // Create a thread for timeout
        Thread timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(3000); // 3-second timeout
            } catch (InterruptedException e) {
                // Timeout thread interrupted
            } finally {
                // If data is not read successfully, close connection
                if (!dataRead[0]) {
                    closeConnection(); // Assuming closeConnection() method is available
                }
            }
        });
        timeoutThread.start();

        // Read data from the input stream
        try {
            bytesCount = mInputStream.read(buffer);
            dataRead[0] = true; // Mark data as read

            // Interrupt the timeout thread since data is successfully read
            timeoutThread.interrupt();
            if (bytesCount > 0) {
                // Convert received bytes to PublicKey
                byte[] decodedKeyBytes = decodePEM(new String(buffer, 0, bytesCount));
                serverPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedKeyBytes));
                //String publicKeyString = Base64.getEncoder().encodeToString(serverPublicKey.getEncoded());
                //System.out.println(publicKeyString);
                //MainActivity.appendToLogTextView("Public key do parceiro RECEBIDA.");
                MainActivity.appendToLogTextView("Partner's public key RECEIVED.");
            }

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            //MainActivity.appendToLogTextView("Public key do parceiro NÃO RECEBIDA.");
            MainActivity.appendToLogTextView("Partner's public key NOT RECEIVED.");
            // Handle error
            e.printStackTrace();
        }


        if (!dataRead[0]) {
            // Não há dados para ler or data not read within timeout
            Log.d("NãoLeu","Não leu a Public Key do Server");
            throw new Exception();
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
            //MainActivity.appendToLogTextView("Dados encriptados NÃO enviados.");
            MainActivity.appendToLogTextView("Encrypted data NOT sent.");
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
//    public byte[] receiveDataEncryptedWithAES() {
//        try {
//            byte[] encryptedData;
//            if (bufferSize == 0){
//                encryptedData = new byte[2048];
//            }else{
//                int nextDivisibleBy32 = ((bufferSize + 31) / 32) * 32;
//
//                encryptedData = new byte[nextDivisibleBy32];
//            }
//            // Tamanho máximo dos dados criptografados
//            int bytesRead = mInputStream.read(encryptedData);
//            if (bytesRead == -1) {
//                // Não há dados para ler
//                return null;
//            }
//
//            // Separar a chave AES e os dados criptografados
//            byte[] encryptedAesKey = Arrays.copyOfRange(encryptedData, 0, 256);
//            byte[] encryptedDataOnly = Arrays.copyOfRange(encryptedData, 256, bytesRead);
//
//            // Descriptografar a chave AES usando a chave privada RSA
//            // Criar uma instância da chave AES
//            SecretKey aesKey = decryptAesKey(encryptedAesKey);
//            //Log.d(TAG, "aesEncodedKey: " + Base64.getEncoder().encodeToString(aesKey.getEncoded()));
//
//
//            // Descriptografar os dados usando a chave AES
//            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
//            cipher.init(Cipher.DECRYPT_MODE, aesKey);
//            byte[] decryptedData = cipher.doFinal(encryptedDataOnly);
//            // Exibir os dados descriptografados
//            //System.out.println("Decrypted data: " + new String(decryptedData, StandardCharsets.UTF_8));
//
//            MainActivity.appendToLogTextView("Dados foram recebidos e desencriptados.");
//            return decryptedData;
//
//        } catch (Exception e) {
//            MainActivity.appendToLogTextView("Dados NÃO foram recebidos.");
//            e.printStackTrace();
//        }
//        return null;
//    }


    public byte[] receiveDataEncryptedWithAES() throws Exception{
        try {
            byte[] encryptedData;
            if (bufferSize == 0){
                encryptedData = new byte[2048];
            } else {
//                int nextDivisibleBy32 = ((bufferSize + 31) / 32) * 32;
//                encryptedData = new byte[nextDivisibleBy32];
                if(bufferSize < 1024){
                    encryptedData = new byte[1024];
                }else{
                    encryptedData = new byte[2048];
                }
            }

            // Create a flag to indicate if data is read successfully
            final boolean[] dataRead = {false};

            // Create a thread for timeout
            Thread timeoutThread = new Thread(() -> {
                try {
                    Thread.sleep(2000); // 2-second timeout
                } catch (InterruptedException e) {
                    // Timeout thread interrupted
                } finally {
                    // If data is not read successfully, close connection
                    if (!dataRead[0]) {
                        closeConnection(); // Assuming closeConnection() method is available
                    }
                }
            });
            timeoutThread.start();

            // Read data from the input stream
            int bytesRead = mInputStream.read(encryptedData);
            dataRead[0] = true; // Mark data as read

            // Interrupt the timeout thread since data is successfully read
            timeoutThread.interrupt();

            if (!dataRead[0] || bytesRead == -1) {
                // Não há dados para ler or data not read within timeout
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

            //MainActivity.appendToLogTextView("Dados foram recebidos e desencriptados.");
            MainActivity.appendToLogTextView("Data has been received and decrypted.");
            return decryptedData;

        } catch (Exception e) {
            //MainActivity.appendToLogTextView("Dados NÃO foram recebidos.");
            MainActivity.appendToLogTextView("Data has NOT been received.");
            e.printStackTrace();
            throw new Exception();
        }
    }

    private SecretKeySpec decryptAesKey(byte[] encryptedAesKey) {
        try {
            // Criar um objeto Cipher para descriptografar com RSA
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());

            // Descriptografar a chave AES
            byte[] aesKeyBytes = cipher.doFinal(encryptedAesKey);
            //MainActivity.appendToLogTextView("Chave AES recebida foi desencriptada");
            MainActivity.appendToLogTextView("AES key received has been decrypted");
            return new SecretKeySpec(aesKeyBytes, "AES");
        } catch (Exception e) {
            //MainActivity.appendToLogTextView("Chave AES recebida NÃO foi desencriptada");
            MainActivity.appendToLogTextView("Received AES key has NOT been decrypted");
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

    public boolean connectToDevice(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Bluetooth device is null");
            return false;
        }

        try {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    return false;
                }
            }
            mBluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            try{
                mBluetoothSocket.connect();
            }catch (IOException connEx){
                closeConnection();
                //MainActivity.appendToLogTextView("Conexao mal sucedida.");
                MainActivity.appendToLogTextView("Unsuccessful connection.");
                return false;
            }

            if (mBluetoothSocket != null && mBluetoothSocket.isConnected()) {
                MainActivity.setBluetoothConnected(true);
                //MainActivity.appendToLogTextView("Bluetooth conectado com o dispositivo.");
                MainActivity.appendToLogTextView("Bluetooth connected to the device.");
                // Socket is connected, now we can obtain our IO streams

                String stringDados = "";
                try {
                    //System.out.println("Estou conectado !!");
                    mInputStream = mBluetoothSocket.getInputStream();
                    mOutputStream = mBluetoothSocket.getOutputStream();

                    //Public keys Exchange
                    //generateKeyPair();
                    sendPublicKey();
                    receiveServerPublicKey();
                    if(!mBluetoothSocket.isConnected()){
                        return false;
                    }
                    ///////////////////////

                    //Data Exchange
                    sendDataEncryptedWithAES(uuid.toString());
                    //MainActivity.appendToLogTextView("UUID encriptado enviado.");
                    MainActivity.appendToLogTextView("Encrypted UUID sent.");
                    //Receive confirmation message
                    String confirmationMessage = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
                    //receiveDataEncryptedWithAES();
                    if(!mBluetoothSocket.isConnected() || !confirmationMessage.equals("Confirmation Message")){
                        return false;
                    }
                    //receiveDataEncryptedWithAES();

                    // Send ready for data encrypted with AES
                    sendDataEncryptedWithAES("Ready for data");
                    //MainActivity.appendToLogTextView("Ready for data ENVIADO");
                    MainActivity.appendToLogTextView("Ready for data SENT");

                    //Receive buffer size
                    receiveBufferSize();
                    if(!mBluetoothSocket.isConnected()){
                        return false;
                    }

//                    //Receive json data and start sync service
//                    String stringDados = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
//                    //System.out.println(stringDados);
//                    sendDataEncryptedWithAES(Integer.toString(countJSONData(stringDados)));

//                    String dataConfirmation = "";
//                    while (!dataConfirmation.equals("Conexao terminada")){
//                        dataConfirmation = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
//                        if(!mBluetoothSocket.isConnected()){
//                            return false;
//                        }
//                        if(dataConfirmation.equals("Num registos incorreto")){
//                            stringDados = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
//                            if(!mBluetoothSocket.isConnected()){
//                                return false;
//                            }
//                            //System.out.println(stringDados);
//                            sendDataEncryptedWithAES(Integer.toString(countJSONData(stringDados)));
//                        }else{
//                            if(processDataJSON(stringDados)){
//                                MainActivity.getInstance().startSyncService();
//                            }
//                        }
//                    }


                    while (!stringDados.equals("Conexao terminada")){
                        //Receive json data and start sync service
                        stringDados = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
                        //System.out.println(stringDados);
                        //MainActivity.appendToLogTextView("Dados recebidos.");
                        MainActivity.appendToLogTextView("Data received.");
                        if (stringDados.equals("Conexao terminada"))
                            break;

                        int countDados = countJSONData(stringDados);
                        if (countDados > 0)
                            processDataJSON(stringDados);
                        sendDataEncryptedWithAES(Integer.toString(countDados));
                        //MainActivity.appendToLogTextView("Enviado num dados");
                        MainActivity.appendToLogTextView("Sent data number");

                        stringDados = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
                        if (stringDados.equals("Conexao terminada"))
                            break;
                        if (!stringDados.equals("Bloco enviado")) {
                            while (stringDados.equals("Num registos incorreto")){
                                //MainActivity.appendToLogTextView("Recebido " +  "(Num registos incorreto)");
                                MainActivity.appendToLogTextView("Received " + "(Incorrect number of records)");
                                stringDados = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
                                if (stringDados.equals("Conexao terminada"))
                                    break;
                            }
                        }else {
                            //MainActivity.appendToLogTextView("Recebido " +  "(Bloco enviado)");
                            MainActivity.appendToLogTextView("Received " + "(Block sent)");
                        }
                    }

                    MainActivity.getInstance().startSyncService();

                    closeConnection();
                    MainActivity.setBluetoothConnected(false);
                    //checkExchangeDataSuccessfuly();
                    ///////////////////////

                } catch (IOException e) {
                    // Handle error
                } catch (Exception e) {
                    if (stringDados.equals("Conexao terminada")){
                        MainActivity.getInstance().startSyncService();
                        return true;
                    }
                    return false;
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
        return true;
    }


    public void receiveBufferSize() throws Exception{
        String bufferSizeString = new String(receiveDataEncryptedWithAES(), StandardCharsets.UTF_8);
        bufferSize = Integer.parseInt(bufferSizeString);
        //MainActivity.appendToLogTextView("Buffersize recebido. Tamanho: " + bufferSize);
        MainActivity.appendToLogTextView("Buffersize received. Size: " + bufferSize);
    }

    public int countJSONData (String receivedData){
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(receivedData);
        } catch (JSONException e) {
            return -1;
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
            //MainActivity.appendToLogTextView("Dados inseridos na BD local.");
            MainActivity.appendToLogTextView("Data inserted into the local DB.");
            System.out.println("Dados inseridos na BD local.");

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