package com.example.simplesocialmediaapp.Models;

public class LocationModel {
    public double latitude;
    public double longitude;
    public long timestamp;

    public LocationModel() {}

    public LocationModel(double latitude, double longitude, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }
}