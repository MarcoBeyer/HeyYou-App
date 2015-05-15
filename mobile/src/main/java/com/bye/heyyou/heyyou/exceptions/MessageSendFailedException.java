package com.bye.heyyou.heyyou.exceptions;

public class MessageSendFailedException extends Exception {
    public MessageSendFailedException(int statusCode){
        super("Unexpected Status Code: "+String.valueOf(statusCode));
    }

}
