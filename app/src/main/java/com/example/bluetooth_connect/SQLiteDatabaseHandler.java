package com.example.bluetooth_connect;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class SQLiteDatabaseHandler extends SQLiteOpenHelper {

    // Database name
    private static final String DATABASE_NAME = "project2_database";

    // Database version
    private static final int DATABASE_VERSION = 1;

    // Table names
    private static final String TABLE_EQUIPMENT = "equipment";
    private static final String TABLE_USERS_DATA = "users_data";

    // Common column names
    private static final String KEY_ID = "id";

    // Equipment table column names
    private static final String KEY_EQUIPMENT_NAME = "name";
    private static final String KEY_EQUIPMENT_LONG = "longitude";
    private static final String KEY_EQUIPMENT_LAT = "latitude";
    private static final String KEY_EQUIPMENT_MAC = "macAddress";

    private static final String[] COLUMNS_TABLE_DEVICES = { KEY_ID, KEY_EQUIPMENT_NAME, KEY_EQUIPMENT_LONG, KEY_EQUIPMENT_LAT, KEY_EQUIPMENT_MAC};

    // Users Data table column names
    private static final String KEY_RECORD_CLASS = "class";
    private static final String KEY_RECORD_TIMESTAMP = "timestamp";
    private static final String KEY_RECORD_DEVICE_ID = "deviceId";
    private static final String KEY_RECORD_IS_SYNCED = "isSynced";

    private static final String[] COLUMNS_TABLE_RECORDS = { KEY_ID, KEY_RECORD_CLASS, KEY_RECORD_TIMESTAMP, KEY_RECORD_DEVICE_ID, KEY_RECORD_IS_SYNCED};

    public SQLiteDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Equipment table
        String CREATE_EQUIPMENT_TABLE = "CREATE TABLE " + TABLE_EQUIPMENT + "("
                + KEY_ID + " VARCHAR(50) PRIMARY KEY,"
                + KEY_EQUIPMENT_NAME + " VARCHAR(150),"
                + KEY_EQUIPMENT_LAT + " DOUBLE,"
                + KEY_EQUIPMENT_LONG + " DOUBLE,"
                + KEY_EQUIPMENT_MAC + " VARCHAR(17) "
                + ")";
        db.execSQL(CREATE_EQUIPMENT_TABLE);

        // Create Users Data table
        String CREATE_USERS_DATA_TABLE = "CREATE TABLE " + TABLE_USERS_DATA + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_RECORD_CLASS + " VARCHAR(30),"
                + KEY_RECORD_TIMESTAMP + " TIMESTAMP,"
                + KEY_RECORD_DEVICE_ID + " VARCHAR(50),"
                + KEY_RECORD_IS_SYNCED + " BOOLEAN DEFAULT 'false'"
                + ")";
        db.execSQL(CREATE_USERS_DATA_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EQUIPMENT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS_DATA);

        // Create tables again
        onCreate(db);
    }

    public void addRecord(Record record){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_RECORD_CLASS, record.getRecordClass());
        values.put(KEY_RECORD_TIMESTAMP, record.getTimestamp().toString());
        values.put(KEY_RECORD_DEVICE_ID, record.getDeviceId());
        values.put(KEY_RECORD_IS_SYNCED, record.is_synced());
        db.insert(TABLE_USERS_DATA,null, values);
        db.close();
    }

    public Record getRecord(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_USERS_DATA, // a. table
                COLUMNS_TABLE_RECORDS, // b. column names
                KEY_ID + " = ?", // c. selections
                new String[] { String.valueOf(id) }, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor != null && cursor.moveToFirst()) {
            String dateString = cursor.getString(2); // Assuming this is your string representation of LocalDateTime
            // Define the expected format of your string
            LocalDateTime timestamp = parseDataToTimestamp(dateString);

            Record record = new Record(
                    Integer.parseInt(cursor.getString(0)),
                    cursor.getString(1),
                    timestamp,
                    cursor.getString(3)
            );

            cursor.close();
            return record;
        } else {
            // If no record found with the given id, return null
            return null;
        }
    }


    public Record getRecord(Record record) {
        if (record == null || record.getId() < 0 ) {
            // Handle the case where the provided Record object or its ID is null
            return null;
        }

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_USERS_DATA, // a. table
                COLUMNS_TABLE_RECORDS, // b. column names
                KEY_ID + " = ?", // c. selections
                new String[] { String.valueOf(record.getId()) }, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor != null && cursor.moveToFirst()) {
            String dateString = cursor.getString(2); // Assuming this is your string representation of LocalDateTime
            LocalDateTime timestamp = parseDataToTimestamp(dateString);

            Record retrievedRecord = new Record(
                    Integer.parseInt(cursor.getString(0)),
                    cursor.getString(1),
                    timestamp,
                    cursor.getString(3)
            );

            cursor.close();
            return retrievedRecord;
        } else {
            // If no record found with the given id, return null
            return null;
        }
    }

    private static LocalDateTime parseDataToTimestamp(String dateString) {
        // Define the expected format of your string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        // Parse the string to LocalDateTime
        return LocalDateTime.parse(dateString, formatter);
    }

    public ArrayList<Record> getRecordNotSynced(){
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Record> records = new ArrayList<Record>();
        Record record = null;

        Cursor cursor = db.query(TABLE_USERS_DATA, // a. table
                COLUMNS_TABLE_RECORDS, // b. column names
                KEY_RECORD_IS_SYNCED + " = ?", // c. selections
                new String[] {String.valueOf(false)}, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String dateString = cursor.getString(2);
                LocalDateTime timestamp = parseDataToTimestamp(dateString);

                record = new Record(
                        Integer.parseInt(cursor.getString(0)),
                        cursor.getString(1),
                        timestamp,
                        cursor.getString(3)
                );
                records.add(record);
            }while (cursor.moveToNext());
        }else{
            return null;
        }
        return records;
    }


    public void updateRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Crie um ContentValues contendo os valores a serem atualizados
        ContentValues values = new ContentValues();
        values.put(KEY_RECORD_IS_SYNCED, record.is_synced() ? 1 : 0); // 1 para true, 0 para false

        // Especifique a cláusula WHERE para identificar o registro a ser atualizado
        String selection = KEY_ID + " = ?";
        String[] selectionArgs = { String.valueOf(record.getId()) };

        // Atualize o registro usando a função update do SQLiteDatabase
        db.update(TABLE_USERS_DATA, values, selection, selectionArgs);

        // Feche a conexão com o banco de dados
        db.close();
    }


    public int deleteAlreadySynced() {
        SQLiteDatabase db = this.getWritableDatabase();

        String whereClause = KEY_RECORD_IS_SYNCED + " = ?";
        String[] whereArgs = { String.valueOf(true) };
        int rowsDeleted = db.delete(TABLE_USERS_DATA, whereClause, whereArgs);

        //Log.d("DeleteSyncedRecords", "Deleted " + rowsDeleted + " rows");

        db.close();
        return  rowsDeleted;
    }

    public ArrayList<Record> getAllRecords() {

        ArrayList<Record> records = new ArrayList<Record>();
        String query = "SELECT  * FROM " + TABLE_USERS_DATA;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Record record = null;

        if (cursor != null && cursor.moveToFirst()) {

            do {
                String dateString = cursor.getString(2); // Assuming this is your string representation of LocalDateTime
                // Define the expected format of your string
                LocalDateTime timestamp = parseDataToTimestamp(dateString);

                 record = new Record(
                        Integer.parseInt(cursor.getString(0)),
                        cursor.getString(1),
                        timestamp,
                        cursor.getString(3)
                );
                records.add(record);
            } while (cursor.moveToNext());
            cursor.close();
            return records;
        } else {
            // If no record found with the given id, return null
            return null;
        }
    }

    public void addDevice(Device device){
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if the device exists in the database
        boolean deviceExists = checkDeviceExists(device.getId(), db);

        ContentValues values = new ContentValues();
        values.put(KEY_ID, device.getId());
        values.put(KEY_EQUIPMENT_NAME, device.getName());
        values.put(KEY_EQUIPMENT_LAT, device.getLatitude());
        values.put(KEY_EQUIPMENT_LONG, device.getLongitude());
        values.put(KEY_EQUIPMENT_MAC, device.getMac());

        if (deviceExists) {
            // Device exists, perform an update
            db.update(TABLE_EQUIPMENT, values, KEY_ID + " = ?", new String[]{String.valueOf(device.getId())});
        } else {
            // Device does not exist, perform an insert
            db.insert(TABLE_EQUIPMENT, null, values);
        }

        db.close();
    }

    // Helper method to check if a device with the given id exists in the database
    private boolean checkDeviceExists(String deviceId, SQLiteDatabase db) {
        String[] columns = {KEY_ID};
        String selection = KEY_ID + " = ?";
        String[] selectionArgs = {deviceId};
        String limit = "1";

        Cursor cursor = db.query(TABLE_EQUIPMENT, columns, selection, selectionArgs, null, null, null, limit);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();

        return exists;
    }

    public Device getDevice(String id){
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_EQUIPMENT, // a. table
                COLUMNS_TABLE_DEVICES, // b. column names
                KEY_ID + " = ?", // c. selections
                new String[] { id }, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor != null && cursor.moveToFirst()) {
            Device device = new Device(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getDouble(2),
                    cursor.getDouble(3),
                    cursor.getString(4)
            );
            cursor.close();
            return device;
        }else {
            // If no record found with the given id, return null
            return null;
        }
    }

    public ArrayList<Device> getAllDevices() {

        ArrayList<Device> devices = new ArrayList<Device>();
        String query = "SELECT  * FROM " + TABLE_EQUIPMENT;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Device device = null;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                device = new Device(cursor.getString(0), cursor.getString(1), cursor.getDouble(2), cursor.getDouble(3), cursor.getString(4));
                devices.add(device);
            } while (cursor.moveToNext());
            cursor.close();
        }else{
            return null;
        }

        return devices;
    }

    public String getDeviceLocation(String id){
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_EQUIPMENT, // a. table
                COLUMNS_TABLE_DEVICES, // b. column names
                KEY_ID + " = ? ", // c. selections
                new String[] { id }, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor != null && cursor.moveToFirst()) {
            cursor.close();
            return cursor.getString(2);
        }else {
            // If no record found with the given id, return null
            return null;
        }
    }

}
