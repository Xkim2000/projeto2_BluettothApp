package com.example.bluetooth_connect;

import java.time.LocalDateTime;

public class Record {
    private int id;
    private String recordClass;
    private LocalDateTime timestamp;
    private String deviceId;
    private boolean isSynced;

    public Record(int id, String recordClass, LocalDateTime timestamp, String deviceId) {
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
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
