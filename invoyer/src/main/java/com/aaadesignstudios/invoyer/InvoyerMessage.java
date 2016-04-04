package com.aaadesignstudios.invoyer;

/**
 * Created by antonioallen on 4/3/16.
 */
public class InvoyerMessage {

    private String senderId;
    private String messageId;
    private String message;
    private long timeStamp;
    private String type;

    public InvoyerMessage(String senderId, String message, String messageId, String type, long timeStamp){
        this.senderId = senderId;
        this.message = message;
        this.type = type;
        this.messageId = messageId;
        this.timeStamp = timeStamp;
    }

    public String getType() {
        return type;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMessage() {
        return message;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
