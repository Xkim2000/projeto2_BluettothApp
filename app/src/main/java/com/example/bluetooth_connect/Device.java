package com.example.bluetooth_connect;

public class Device {
    private String id;
    private String name;
    private Double latitude;
    private Double longitude;

    public Device(String id, String name, Double latitude, Double longitude) {
        setId(id);
        setName(name);
        setLatitude(latitude);
        setLongitude(longitude);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
}
