package com.bye.heyyou.heyyou.message;


import java.sql.Timestamp;

public class OutgoingUserMessage extends UserMessage {
    private boolean sent;

    public OutgoingUserMessage(String messageId,String fromUserID,String toUserID,String content,MessageTypes messageType, Timestamp sentTime, boolean read, boolean sent){
        super(messageId,fromUserID,toUserID,content,messageType,sentTime,read);
        this.sent=sent;
    }

    public boolean isSent() {
        return sent;
    }
}

