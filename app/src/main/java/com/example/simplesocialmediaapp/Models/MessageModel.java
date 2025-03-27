package com.example.simplesocialmediaapp.Models;

public class MessageModel {
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;

    // location in chat
    private Double latitude;
    private Double longitude;
    private boolean isLocation; // true = location msg

    // No-argument constructor required for Firebase serialization
    public MessageModel() {
    }

    public MessageModel(String senderId, String receiverId, String message, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
    }

    // getters and setters for location
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public boolean isLocation() { return isLocation; }
    public void setLocation(boolean location) { isLocation = location; }



        // Getters and setters
    public String getSenderId() {
        return senderId;
    }
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    public String getReceiverId() {
        return receiverId;
    }
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
