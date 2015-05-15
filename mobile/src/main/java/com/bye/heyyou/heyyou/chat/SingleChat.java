package com.bye.heyyou.heyyou.chat;

import com.bye.heyyou.heyyou.message.UserMessage;
import com.bye.heyyou.heyyou.user.User;

import java.util.ArrayList;
import java.util.List;

public class SingleChat {
    private List<UserMessage> messages=new ArrayList<UserMessage>();
    private User opponentUser;

    protected SingleChat(User opponentUser){
        this.opponentUser=opponentUser;
    }

    protected SingleChat(List<UserMessage> messages,User opponentUser){
        this.messages=messages;
        this.opponentUser=opponentUser;
    }

    //TODO implement
    public List<UserMessage> getMessagesOrderedByTimeStamp() {
        throw new UnsupportedOperationException();
    }

    public void addMessage(UserMessage newMessage) {
        messages.add(newMessage);
    }

    public User getOpponentUser(){
      return opponentUser;
    }

    public String getOpponentUserID(){
      return opponentUser.getUserID();
    }

    public List<UserMessage> getMessages() {
        return messages;
    }

    public void clear(){messages.clear();}

    @Override
    public boolean equals(Object other) {
        if (this == other){
            return true;
        }
        if (other == null) {
            return false;
        }
        if (other.getClass() != getClass()) {
            return false;
        }
        return this.opponentUser.getUserID().equals(((SingleChat) other).getOpponentUserID());
    }
}
