package com.bye.heyyou.heyyou.message;

import java.sql.Timestamp;

/**
 * Created by bea on 09.12.2014.
 */


//CREATE TABLE IncomingUserMessages(ToUserID text, FromUserID text,Content text,MessageID String PRIMARY KEY,MessageType text,SentTime text,Read boolean)
public class IncomingUserMessage extends UserMessage {
    public IncomingUserMessage(String messageId,String fromUserID,String toUserID,String content,MessageTypes messageType, Timestamp sentTime, boolean read){
        super(messageId,fromUserID,toUserID,content,messageType,sentTime,read);
    }
}
