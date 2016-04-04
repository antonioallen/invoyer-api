package com.aaadesignstudios.invoyer;

/**
 * Created by antonioallen on 4/3/16.
 */
public class InvoyerMessage {

    private String senderId;
    private String messageId;
    private String message;
    private long timeStamp;

    public InvoyerMessage(String senderId, String message, String messageId, long timeStamp){
        this.senderId = senderId;
        this.message = message;
        this.messageId = messageId;
        this.timeStamp = timeStamp;
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
