package com.bye.heyyou.heyyou.message;

import java.sql.Timestamp;

public abstract class UserMessage {
    protected String fromUserID;
    protected String toUserId;
    protected String content;
    protected MessageTypes messageType;
    protected Timestamp sentTime;
    protected String messageId;
    protected boolean read;

    /**
     *
     * @param fromUserID
     * @param toUserID
     * @param content
     * @param messageType
     * @param sentTime
     */
    public UserMessage(String messageId,String fromUserID,String toUserID,String content,MessageTypes messageType, Timestamp sentTime, boolean read){
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
