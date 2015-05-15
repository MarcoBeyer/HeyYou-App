package com.bye.heyyou.heyyou.chat;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bye.heyyou.heyyou.R;
import com.bye.heyyou.heyyou.exceptions.NoNewMessageException;
import com.bye.heyyou.heyyou.message.IncomingUserMessage;
import com.bye.heyyou.heyyou.message.MessageTypes;
import com.bye.heyyou.heyyou.message.OutgoingUserMessage;
import com.bye.heyyou.heyyou.message.UserMessage;
import com.bye.heyyou.heyyou.message.UserMessageTimeComparator;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalMessageHistoryDatabase extends SQLiteOpenHelper {
    private Context context;

    public LocalMessageHistoryDatabase(Context context) {
        super(
                context,
                "localMessageDB",
                null, Integer.parseInt(context.getResources().getString(R.string.database_version))
        );
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IncomingUserMessages(ToUserID text, FromUserID text,Content text,MessageID String PRIMARY KEY,MessageType text,SentTime text,Read integer)");
        db.execSQL("CREATE TABLE OutgoingUserMessages(ToUserID text, FromUserID text,Content text,MessageID String PRIMARY KEY,MessageType text,SentTime text, Sent integer, Read integer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 3 && newVersion == 4) {
            db.execSQL("DROP TABLE UserMessages;");
            db.execSQL("CREATE TABLE IncomingUserMessages(ToUserID text, FromUserID text,Content text,MessageID String PRIMARY KEY,MessageType text,SentTime text,Read integer)");
            db.execSQL("CREATE TABLE OutgoingUserMessages(ToUserID text, FromUserID text,Content text,MessageID String PRIMARY KEY,MessageType text,SentTime text, Sent integer, Read integer)");
        }
    }

    public int generateNewMessageID() {
        int lastID = PreferenceManager.getDefaultSharedPreferences(context).getInt("messageID", 0);
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt("messageID", lastID+1).commit();
        return lastID+1;
    }

    public List<UserMessage> getMessages() throws NoNewMessageException {
        String sql = "SELECT * FROM IncomingUserMessages;";
        String sql2 = "SELECT * FROM OutgoingUserMessages;";
        List<IncomingUserMessage> incomingUserMessages = parseIncomingMessages(sql);
        List<OutgoingUserMessage> outgoingUserMessages = parseOutgoingMessages(sql2);
        List<UserMessage> messages = new ArrayList<>();
        messages.addAll(incomingUserMessages);
        messages.addAll(outgoingUserMessages);

        Collections.sort(messages, new UserMessageTimeComparator());

        if (messages.size() > 0) {
            return messages;
        }
        throw new NoNewMessageException();
    }

    public void markAsSent(String messageId){
        String sql = "UPDATE OutgoingUserMessages SET Sent=1 WHERE messageID= "+messageId+"';";
        Log.d("LocalMessageDB","Message als Sent markiert: " + messageId);
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(sql);
    }

    public List<UserMessage> getMessagesWithOppositeUser(String opponentUserID) throws NoNewMessageException {
        String sql = "SELECT * FROM IncomingUserMessages WHERE ToUserID='" + opponentUserID + "' OR FromUserID='" + opponentUserID + "';";
        String sql2 = "SELECT * FROM OutgoingUserMessages WHERE ToUserID='" + opponentUserID + "' OR FromUserID='" + opponentUserID + "';";
        List<UserMessage> messages = new ArrayList<>();
        List<IncomingUserMessage> incomingUserMessages = parseIncomingMessages(sql);
        List<OutgoingUserMessage> outgoingUserMessages = parseOutgoingMessages(sql2);
        messages.addAll(incomingUserMessages);
        messages.addAll(outgoingUserMessages);
        Collections.sort(messages, new UserMessageTimeComparator());
        if (messages.size() > 0) {
            return messages;
        }
        throw new NoNewMessageException();
    }

    private List<IncomingUserMessage> parseIncomingMessages(String sql) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<IncomingUserMessage> messages = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                String fromUserID = cursor.getString(cursor.getColumnIndex("FromUserID"));
                String toUserId = cursor.getString(cursor.getColumnIndex("ToUserID"));
                String content = cursor.getString(cursor.getColumnIndex("Content"));
                MessageTypes messageType = MessageTypes.valueOf(cursor.getString(cursor.getColumnIndex("MessageType")));
                Timestamp sentTime = Timestamp.valueOf(cursor.getString(cursor.getColumnIndex("SentTime")));
                String messageID = cursor.getString(cursor.getColumnIndex("MessageID"));
                boolean read = false;
                if (1 == cursor.getInt(cursor.getColumnIndex("Read"))) {
                    read = true;
                }
                IncomingUserMessage message = new IncomingUserMessage(messageID, fromUserID, toUserId, content, messageType, sentTime, read);
                messages.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return messages;
    }

    private List<OutgoingUserMessage> parseOutgoingMessages(String sql) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<OutgoingUserMessage> messages = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                String fromUserID = cursor.getString(cursor.getColumnIndex("FromUserID"));
                String toUserId = cursor.getString(cursor.getColumnIndex("ToUserID"));
                String content = cursor.getString(cursor.getColumnIndex("Content"));
                MessageTypes messageType = MessageTypes.valueOf(cursor.getString(cursor.getColumnIndex("MessageType")));
                Timestamp sentTime = Timestamp.valueOf(cursor.getString(cursor.getColumnIndex("SentTime")));
                String messageID = cursor.getString(cursor.getColumnIndex("MessageID"));
                boolean read = false;
                if (1 == cursor.getInt(cursor.getColumnIndex("Read"))) {
                    read = true;
                }
                boolean sent = false;
                if (1 == cursor.getInt(cursor.getColumnIndex("Sent"))) {
                    sent = true;
                }
                OutgoingUserMessage message = new OutgoingUserMessage(messageID, fromUserID, toUserId, content, messageType, sentTime, read, sent);
                messages.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return messages;
    }

    public void addNewOutgoingMessage(OutgoingUserMessage newMessage) {
        Log.d("LocalMessageHistoryDB", "new Message from " + newMessage.getFromUserId() + " with content " + newMessage.getContent());
        String sql = "insert into OutgoingUserMessages(ToUserID,FromUserID,Content,MessageID,MessageType,SentTime,Sent,Read) values (" + "'" + newMessage.getToUserId() + "','" + newMessage.getFromUserId() + "','" + newMessage.getContent() + "','" + newMessage.getMessageId() + "','" + newMessage.getMessageType() + "','" + newMessage.getSentTime() + "','" + mapBool(newMessage.isRead()) + "','" + mapBool(newMessage.isSent()) + "');";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(sql);
    }

    public void addNewIncomingMessage(IncomingUserMessage newMessage) {
        Log.d("LocalMessageHistoryDB", "new Message from " + newMessage.getFromUserId() + " with content " + newMessage.getContent());
        String sql = "insert into IncomingUserMessages(ToUserID,FromUserID,Content,MessageID,MessageType,SentTime,Read) values (" + "'" + newMessage.getToUserId() + "','" + newMessage.getFromUserId() + "','" + newMessage.getContent() + "','" + newMessage.getMessageId() + "','" + newMessage.getMessageType() + "','" + newMessage.getSentTime() + "','" + mapBool(newMessage.isRead()) + "');";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(sql);
    }

    public void deleteChatWithUser(String opponentUserID) {
        String sql = "DELETE FROM IncomingUserMessages WHERE ToUserID='" + opponentUserID + "' OR FromUserID='" + opponentUserID + "';";
        String sql2 = "DELETE FROM OutgoingUserMessages WHERE ToUserID='" + opponentUserID + "' OR FromUserID='" + opponentUserID + "';";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(sql);
        db.execSQL(sql2);
    }

    public void deleteAllChats() {
        String sql = "DELETE FROM IncomingUserMessages";
        String sql2 = "DELETE FROM OutgoingUserMessages";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(sql);
        db.execSQL(sql2);
    }

    private int mapBool(boolean bool) {
        if (bool)
        {
            return 1;
        }
        return 0;
    }
}