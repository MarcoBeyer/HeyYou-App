package com.bye.heyyou.heyyou;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bye.heyyou.heyyou.chat.ChatManager;
import com.bye.heyyou.heyyou.chat.SingleChat;
import com.bye.heyyou.heyyou.exceptions.ChatNotFoundException;
import com.bye.heyyou.heyyou.message.MessageTypes;
import com.bye.heyyou.heyyou.message.UserMessage;
import com.bye.heyyou.heyyou.notifications.MessageNotificationManager;

import java.util.List;
import java.util.Observable;
import java.util.Observer;


public class ChatActivity extends Activity  implements Observer {
    private SingleChat chat;
    private String opponentUserId = "";
    private ChatManager chatManager;
    private String myUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        if(savedInstanceState != null){
            return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Bundle bundle = getIntent().getExtras();
        myUserID = PreferenceManager.getDefaultSharedPreferences(ChatActivity.this).getString("userId", null);
        String password = PreferenceManager.getDefaultSharedPreferences(this).getString("password", null);
        String userMessageServerUrl = PreferenceManager.getDefaultSharedPreferences(this).getString("userMessageServerUrl", null);
        opponentUserId = bundle.getString("OpponentUserId");
        getActionBar().setTitle(opponentUserId);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Log.d("Chat Activity", "Opponent User: " + opponentUserId);
        setOpenUserChatWithUserID(opponentUserId);
        chatManager = new ChatManager(this, myUserID, password, userMessageServerUrl);
        chatManager.addObserver(this);
        try {
            chat = chatManager.getChatWithUser(opponentUserId);
        } catch (ChatNotFoundException e) {
            chat = chatManager.createNewChat(opponentUserId);
        }
        displayChat();
    }

    @Override
    protected void onStop(){
        super.onStop();
        setOpenUserChatWithUserID("");
        chatManager.unregister();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        setOpenUserChatWithUserID("");
        chatManager.unregister();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new MessageNotificationManager(this).resetNumberNewMessages();
        new MessageNotificationManager(this).deleteNotification();
        chatManager.resume();
    }


    @Override
    protected void onPause() {
        super.onPause();
            chatManager.pause();
    }

    private void setOpenUserChatWithUserID(String opponentUserID){
        SharedPreferences settings = getSharedPreferences("active chat",MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("ChatOpenWithUser", opponentUserID);
        Log.d("Chatmanager","set open Chat to: "+opponentUserID);
        editor.apply();
    }

    private void displayChat() {
        List<UserMessage> messages = chat.getMessages();
        LinearLayout activeChatViewContainer = (LinearLayout) findViewById(R.id.activeChat);
        activeChatViewContainer.removeAllViews();
        for (UserMessage message : messages) {
            LayoutInflater inflater = LayoutInflater.from(this);
            activeChatViewContainer = (LinearLayout) findViewById(R.id.activeChat);
            View chatRowView = null;
            if (message.getMessageType() == MessageTypes.TEXT) {
                if (message.getFromUserId().equals(opponentUserId)) {
                    chatRowView = inflater.inflate(R.layout.chat_messagerow_incoming_text, activeChatViewContainer, false);
                }
                if (message.getFromUserId().equals(myUserID)) {
                    chatRowView = inflater.inflate(R.layout.chat_messagerow_outgoing_text, activeChatViewContainer, false);
                }
                if (chatRowView == null) {
                    return;
                }
                TextView messageView = (TextView) chatRowView.findViewById(R.id.message);
                messageView.setText(message.getContent());
                activeChatViewContainer.addView(chatRowView);
            }

        }
        //Scroll Down
        final ScrollView mScrollView = (ScrollView) findViewById(R.id.messageScroller);
        mScrollView.post(new Runnable() {
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    public void onSendButtonClick(View view) {
        TextView messageInputField = (TextView) findViewById(R.id.messageInputField);
        if(!messageInputField.getText().toString().matches("\\s*$")) {
            String messageToSend = String.valueOf(messageInputField.getText());
            messageInputField.setText("");
            chatManager.sendMessage(opponentUserId, messageToSend, MessageTypes.TEXT);
            displayChat();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void update(Observable observable, Object data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayChat();
            }
        });
    }
}
