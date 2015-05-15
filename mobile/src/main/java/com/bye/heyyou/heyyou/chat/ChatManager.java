package com.bye.heyyou.heyyou.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.bye.heyyou.heyyou.exceptions.ChatNotFoundException;
import com.bye.heyyou.heyyou.exceptions.NoNewMessageException;
import com.bye.heyyou.heyyou.message.MessageTypes;
import com.bye.heyyou.heyyou.message.OutgoingUserMessage;
import com.bye.heyyou.heyyou.message.UserMessage;
import com.bye.heyyou.heyyou.service.xmpp.XMPPServiceConnection;
import com.bye.heyyou.heyyou.user.User;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class ChatManager extends Observable {
    private boolean mBroadcastRegistered;
    private List<SingleChat> chats = new ArrayList<>();
    private LocalMessageHistoryDatabase localMessageHistoryDatabase;
    private XMPPServiceConnection xmppServiceConnection;
    private Context context;
    private String myUserID;
    private NewMessageReceiver newMessageReceiver;

    public ChatManager(Context context,String myUserID, String password, String messageDBAddress) {
        this.myUserID = myUserID;
        localMessageHistoryDatabase = new LocalMessageHistoryDatabase(context);
        xmppServiceConnection = new XMPPServiceConnection(context,myUserID,password, messageDBAddress);
        this.context = context;
        try {
            List<UserMessage> newMessages = localMessageHistoryDatabase.getMessages();
            localMessageHistoryDatabase.close();
            addMessages(newMessages);
        } catch (NoNewMessageException e) {
            Log.d("Message", "No new Message");
        }
        registerBroadcastReceiver();
    }

    private void registerBroadcastReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.bye.heyyou.heyyou.NEW_MESSAGE");
        newMessageReceiver = new NewMessageReceiver();
        context.registerReceiver(newMessageReceiver,intentFilter);
        mBroadcastRegistered = true ;
    }

    public SingleChat createNewChat(String OpponentUserId) {
        SingleChat newChat = new SingleChat(new User(OpponentUserId));
        chats.add(newChat);
        return newChat;
    }

    public void unregister(){
        localMessageHistoryDatabase.close();
        if (mBroadcastRegistered) {
            context.unregisterReceiver(newMessageReceiver);
            mBroadcastRegistered=false;
        }
        xmppServiceConnection.close();
    }


    public void pause(){
        xmppServiceConnection.close();
    }

    public void resume(){
        xmppServiceConnection.open();
        try {
            List<UserMessage> newMessages = localMessageHistoryDatabase.getMessages();
            localMessageHistoryDatabase.close();
            addMessages(newMessages);
        } catch (NoNewMessageException e) {
            Log.d("Message", "No new Message");
        }
    }

    public List<SingleChat> getActiveChats() {
        return chats;
    }

    public SingleChat getChatWithUser(String UserID) throws ChatNotFoundException {
        for (SingleChat singleChat : chats) {
            if (singleChat.getOpponentUserID().equals(UserID)) {
                return singleChat;
            }
        }
        throw new ChatNotFoundException();
    }

    private void addMessages(List<UserMessage> newMessages) {
        for (UserMessage message : newMessages) {
            addMessage(message);
        }
    }

    public void deleteChatWithUserID(String userId){
        localMessageHistoryDatabase.deleteChatWithUser(userId);
        for(SingleChat chat: chats){
            if (chat.getOpponentUserID().equals(userId)){
                chats.remove(chat);
            }
        }
        setChanged();
        notifyObservers();
    }

    public void deleteAllChats(){
        localMessageHistoryDatabase.deleteAllChats();
        setChanged();
        notifyObservers();
    }

    private void addMessage(UserMessage message) {
        if (!message.getFromUserId().equals(myUserID)) {
            try {
                getChatWithUser(message.getFromUserId()).addMessage(message);
            } catch (ChatNotFoundException e) {
                SingleChat newChat = createNewChat(message.getFromUserId());
                newChat.addMessage(message);
            }
        } else if (message.getFromUserId().equals(myUserID)) {
            try {
                getChatWithUser(message.getToUserId()).addMessage(message);
            } catch (ChatNotFoundException e) {
                SingleChat newChat = createNewChat(message.getToUserId());
                newChat.addMessage(message);
            }
        }
    }

    public void sendMessage(String toUserId, String content, MessageTypes messageType) {
        java.util.Date utilDate = new java.util.Date();
        Timestamp actualSqlDate = new Timestamp(utilDate.getTime());
        final OutgoingUserMessage newMessage = new OutgoingUserMessage(String.valueOf(localMessageHistoryDatabase.generateNewMessageID()),myUserID, toUserId, content, messageType, actualSqlDate,false,false);
        sendMessage(newMessage);
    }

    public void sendMessage(OutgoingUserMessage userMessage) {
        localMessageHistoryDatabase.addNewOutgoingMessage(userMessage);
        //add Message to current Chat
        addMessage(userMessage);
        xmppServiceConnection.sendMessage(userMessage);
        setChanged();
        notifyObservers();
    }

    public class NewMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            for (SingleChat chat : chats) {
                chat.clear();
            }
            try {
                List<UserMessage> newMessages = localMessageHistoryDatabase.getMessages();
                localMessageHistoryDatabase.close();
                addMessages(newMessages);
            } catch (NoNewMessageException e) {
                Log.d("Message", "No new Message");
            }
            setChanged();
            notifyObservers();
        }
    }
}
