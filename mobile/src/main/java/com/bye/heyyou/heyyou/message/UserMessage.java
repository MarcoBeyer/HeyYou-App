package com.bye.heyyou.heyyou.message;

import java.sql.Timestamp;

public abstract class UserMessage {
    private String fromUserID;
    private String toUserId;
    private String content;
    private MessageTypes messageType;
    private Timestamp sentTime;
    private String messageId;
    private boolean read;

    /**
     *
     * @param fromUserID
     * @param toUserID
     * @param content
     * @param messageType
     * @param sentTime
     */
    UserMessage(String messageId, String fromUserID, String toUserID, String content, MessageTypes messageType, Timestamp sentTime, boolean read){
        this.fromUserID=fromUserID;
        this.toUserId=toUserID;
        this.content=content;
        this.messageType=messageType;
        this.messageId=messageId;
        this.sentTime=sentTime;
        this.read =read ;
    }

    public String getMessageId(){return messageId;}

    public String getToUserId() {
        return toUserId;
    }

    public String getFromUserId() {
        return fromUserID;
    }

    public String getContent() {
        return content;
    }

    public MessageTypes getMessageType() {
        return messageType;
    }

    public Timestamp getSentTime() {
        return sentTime;
    }

    public boolean isRead() {
        return read;
    }
}
