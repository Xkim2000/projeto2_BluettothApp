package com.example.bluetooth_connect;

import java.time.LocalDateTime;

public class Record {
    private int id;
    private String recordClass;
    private String timestamp;
    private String deviceId;
    private boolean isSynced;

    public Record(int id, String recordClass, String timestamp, String deviceId, boolean isSynced) {
        setId(id);
        setRecordClass(recordClass);
        setTimestamp(timestamp);
        setDeviceId(deviceId);
        setIsSynced(isSynced);
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

    @Override
    public String toString() {
        return "Record{" +
                "id=" + id +
                ", recordClass='" + recordClass + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", isSynced=" + isSynced +
                '}';
    }
}
