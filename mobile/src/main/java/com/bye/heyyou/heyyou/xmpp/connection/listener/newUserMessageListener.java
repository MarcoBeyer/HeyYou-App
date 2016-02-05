package com.bye.heyyou.heyyou.xmpp.connection.listener;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.bye.heyyou.heyyou.database.LocalMessageHistoryDatabase;
import com.bye.heyyou.heyyou.message.IncomingUserMessage;
import com.bye.heyyou.heyyou.message.MessageTypes;
import com.bye.heyyou.heyyou.notifications.MessageNotificationManager;

import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;

import java.sql.Timestamp;


public class newUserMessageListener implements ChatMessageListener{
    private Context context;
    private MessageNotificationManager messageNotificationManager;

    public newUserMessageListener(Context context){
        this.context=context;
        messageNotificationManager = new MessageNotificationManager(context);
    }

    @Override
    public void processMessage(Chat chat, Message message) {
        if (message.getBody() != null) {
            Log.d("XMPP", "Received message: " + message);
            LocalMessageHistoryDatabase localMessageHistoryDatabase = new LocalMessageHistoryDatabase(context);
            java.util.Date utilDate = new java.util.Date();
            Timestamp actualSqlDate = new Timestamp(utilDate.getTime());
            IncomingUserMessage incomingUserMessage = new IncomingUserMessage(String.valueOf(localMessageHistoryDatabase.generateNewMessageID()), message.getFrom().getLocalpartOrNull().toString(), message.getTo().getLocalpartOrNull().toString(), message.getBody(), MessageTypes.TEXT, actualSqlDate, false);
            localMessageHistoryDatabase.addNewIncomingMessage(incomingUserMessage);
            localMessageHistoryDatabase.close();
            notifyNewMessage(message);
            Intent i = new Intent("com.bye.heyyou.heyyou.NEW_MESSAGE");
            context.sendBroadcast(i);
        }
    }

    private void notifyNewMessage(Message message) {
        if (!isChatWithUserOpen(message.getFrom().getLocalpartOrNull().toString())) {
            messageNotificationManager.increaseNumberNewMessages();
            messageNotificationManager.showNewMessage(message);
        }
    }

    private boolean isChatWithUserOpen(String opponentUserId) {
        SharedPreferences settings = context.getSharedPreferences("active chat", Context.MODE_PRIVATE);
        return settings.getString("ChatOpenWithUser", "").equals(opponentUserId);
    }

}