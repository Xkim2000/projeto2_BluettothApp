package com.example.bluetooth_connect;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
    private static final String KEY_EQUIPMENT_LOCATION = "location";

    // Users Data table column names
    private static final String KEY_RECORD_CLASS = "class";
    private static final String KEY_RECORD_TIMESTAMP = "timestamp";
    private static final String KEY_RECORD_DEVICE_ID = "deviceId";
    private static final String KEY_RECORD_IS_SYNCED = "isSynced";

    public SQLiteDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Equipment table
        String CREATE_EQUIPMENT_TABLE = "CREATE TABLE " + TABLE_EQUIPMENT + "("
                + KEY_ID + " VARCHAR(50) PRIMARY KEY,"
                + KEY_EQUIPMENT_NAME + " VARCHAR(150),"
                + KEY_EQUIPMENT_LOCATION + " TEXT"
                + ")";
        db.execSQL(CREATE_EQUIPMENT_TABLE);

        // Create Users Data table
        String CREATE_USERS_DATA_TABLE = "CREATE TABLE " + TABLE_USERS_DATA + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_RECORD_CLASS + " VARCHAR(30),"
                + KEY_RECORD_TIMESTAMP + " TIMESTAMP,"
                + KEY_RECORD_DEVICE_ID + " VARCHAR(50),"
                + KEY_RECORD_IS_SYNCED + " BOOLEAN"
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
    }

    //TODO getRecord(), getRecordNotSynced(), deleteAlreadySynced()

}
