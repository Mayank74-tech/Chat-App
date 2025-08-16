package com.java.chatapplication.models;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String message;
    private String senderName;
    private String senderImage;
    private long timestamp;
    private boolean isSeen;

    public Message() {
        // Required empty constructor for Firebase
    }

    public Message(String messageId, String senderId, String receiverId,
                   String senderName, String senderImage,
                   String message, long timestamp, boolean isSeen) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderName = senderName;
        this.senderImage = senderImage;
        this.message = message;
        this.timestamp = timestamp;
        this.isSeen = isSeen;
    }
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

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
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderImage() { return senderImage; }
    public void setSenderImage(String senderImage) { this.senderImage = senderImage; }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSeen() {
        return isSeen;
    }

    public void setSeen(boolean seen) {
        isSeen = seen;
    }
}
