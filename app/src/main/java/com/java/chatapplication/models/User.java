package com.java.chatapplication.models;

public class User {
    private String uid;
    private String username;
    private String email;
    private String profileImageUrl;
    private String status;
    private Object lastChanged; // Server timestamp
    private int unreadCount;
    // NEW

    public User() {
        // Empty constructor for Firebase
    }

    public User(String uid, String username, String email, String profileImageUrl) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.status = "online";
        this.lastChanged = null;
        this.unreadCount = 0;
    }

    // Getters
    public String getUid() { return uid; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getStatus() { return status; }
    public Object getLastChanged() { return lastChanged; }
    public int getUnreadCount() { return unreadCount; }

    // Setters
    public void setUid(String uid) { this.uid = uid; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setStatus(String status) { this.status = status; }
    public void setLastChanged(Object lastChanged) { this.lastChanged = lastChanged; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
