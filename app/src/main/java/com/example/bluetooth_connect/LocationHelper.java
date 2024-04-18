package com.example.bluetooth_connect;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class LocationHelper {
    private static final String TAG = LocationHelper.class.getSimpleName();
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 30; // 30 meters
    private static final long MIN_TIME_BW_UPDATES = 1000 * 20; // 20 secs minute

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Context context;

    private double destinationLat;
    private double destinationLng;

    private String macAddress;

    public LocationHelper(Context context, double destLat, double destLng, String macAddress) {
        this.context = context;
        this.destinationLat = destLat;
        this.destinationLng = destLng;
        this.macAddress = macAddress;

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // Call your method here to check distance
                checkDistance(location.getLatitude(), location.getLongitude(), destinationLat, destinationLng, macAddress);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
    }

    public void startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    locationListener
            );

            // Start the Foreground Service
            Intent serviceIntent = new Intent(context, LocationForegroundService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted: " + e.getMessage());
        }
    }

    public void stopLocationUpdates() {
        locationManager.removeUpdates(locationListener);

        // Stop the Foreground Service
        Intent serviceIntent = new Intent(context, LocationForegroundService.class);
        context.stopService(serviceIntent);
    }

    private void checkDistance(double currentLat, double currentLng, double destinationLat, double destinationLng, String macAddress) {
        float[] results = new float[1];
        Location.distanceBetween(currentLat, currentLng, destinationLat, destinationLng, results);
        float distanceInMeters = results[0];

        if (distanceInMeters <= 30) {
            MainActivity mainActivity = MainActivity.getInstance();
            MainActivity.appendToLogTextView("Raspberry DENTRO do alcance para comunicação. Em " + distanceInMeters + " metros.");
            // You are within 30 meters of the destination
            Log.d(TAG, "Within 30 meters of destination!");
            // Do whatever action you want here, like logging
            mainActivity.makeConnection();
        }else{
            MainActivity.appendToLogTextView("Raspberry FORA do alcance para comunicação. Em " + distanceInMeters);
        }

    }
}




