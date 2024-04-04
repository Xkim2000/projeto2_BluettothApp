package com.example.bluetooth_connect;

import java.time.LocalDateTime;

public class Record_Send {

    private int id;
    private String recordClass;
    private String timestamp;
    private String deviceId;
    private boolean isSynced;

    public Record_Send(int id, String recordClass, String timestamp, String deviceId) {
        setId(id);
        setRecordClass(recordClass);
        setTimestamp(timestamp);
        setDeviceId(deviceId);
        setIsSynced(false);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRecordClass() {
        return recordClass;
    }

    public void setRecordClass(String recordClass) {
        this.recordClass = recordClass;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean is_synced() {
        return isSynced;
    }

    public void setIsSynced(boolean is_synced) {
        this.isSynced = is_synced;
    }
}
