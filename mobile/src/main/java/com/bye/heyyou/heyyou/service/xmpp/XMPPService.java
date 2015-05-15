package com.bye.heyyou.heyyou.service.xmpp;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.bye.heyyou.heyyou.chat.LocalMessageHistoryDatabase;
import com.bye.heyyou.heyyou.message.IncomingUserMessage;
import com.bye.heyyou.heyyou.message.MessageTypes;
import com.bye.heyyou.heyyou.message.OutgoingUserMessage;
import com.bye.heyyou.heyyou.notifications.MessageNotificationManager;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jxmpp.jid.JidWithLocalpart;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class XMPPService extends Service {
    private AbstractXMPPConnection conn;
    private ChatManager chatmanager;
    private final IBinder xmppServiceBinder = new XMPPServiceBinder();
    private ExecutorService xmppConnectionThreads;
    private MessageNotificationManager messageNotificationManager;

    private String getUserID() {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        return settings.getString("userID", null);
    }

    private void setUserID(String userID) {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("userID", userID);
        editor.apply();
    }

    private String getPassword() {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        return settings.getString("password", null);
    }

    private void setPassword(String password) {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("password", password);
        editor.apply();
    }

    private String getDbAddress() {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        return settings.getString("dbAddress", null);
    }

    private void setDbAddress(String dbAddress) {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("dbAddress", dbAddress);
        editor.apply();
    }

    @Override
    public void onCreate() {
        xmppConnectionThreads = Executors.newSingleThreadExecutor();
        messageNotificationManager = new MessageNotificationManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras;
        boolean mChanged = false;
        if (intent != null) {
            extras = intent.getExtras();
            if (extras != null) {
                if (extras.getString("password") != null) {
                    if (!extras.getString("password").equals(getPassword())) {
                        setPassword(extras.getString("password"));
                        mChanged = true;
                    }
                }
                if (extras.getString("userID") != null) {
                    if (!extras.getString("userID").equals(getUserID())) {
                        setUserID(extras.getString("userID"));
                        mChanged = true;
                    }
                }

                if (extras.getString("dbAddress") != null) {
                    if (!extras.getString("dbAddress").equals(getDbAddress())) {
                        setDbAddress(extras.getString("dbAddress"));
                        mChanged = true;
                    }
                }
            }
        }

        if (getPassword() != null && getUserID() != null && getDbAddress() != null) {
            Log.d("Service", "Service started");
            if (mChanged) {
                if (conn != null && conn.isConnected()) {
                    conn.disconnect();
                }
                xmppConnectionThreads.submit(new Setup());
                xmppConnectionThreads.submit(new Connect());
                xmppConnectionThreads.submit(new Login());
            } else if (conn == null) {
                xmppConnectionThreads.submit(new Setup());
            }
            if ((conn == null || !conn.isConnected() || !conn.isAuthenticated()) && !mChanged) {
                Log.d("XMPP", "Connect and login");
                xmppConnectionThreads.submit(new Connect());
                xmppConnectionThreads.submit(new Login());
            }
        } else
            Log.e("XMPPService", "no login credentials supplied");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        xmppConnectionThreads.shutdown();
    }


    private void setNewMessageListener() {
        chatmanager = ChatManager.getInstanceFor(conn);
        chatmanager.addChatListener(
                new ChatManagerListener() {
                    @Override
                    public void chatCreated(Chat chat, boolean createdLocally) {
                        if (!createdLocally)
                            chat.addMessageListener(new newUserMessageListener());
                    }
                });
    }

    @Override
    public IBinder onBind(Intent intent) {
        messageNotificationManager.resetNumberNewMessages();
        messageNotificationManager.deleteNotification();
        return xmppServiceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        return false;
    }


    public class newUserMessageListener implements ChatMessageListener {
        @Override
        public void processMessage(Chat chat, Message message) {
            if (message.getBody() != null) {
                Log.d("XMPP", "Received message: " + message);
                LocalMessageHistoryDatabase localMessageHistoryDatabase = new LocalMessageHistoryDatabase(getBaseContext());
                java.util.Date utilDate = new java.util.Date();
                Timestamp actualSqlDate = new Timestamp(utilDate.getTime());
                IncomingUserMessage incomingUserMessage = new IncomingUserMessage(String.valueOf(localMessageHistoryDatabase.generateNewMessageID()), message.getFrom().maybeGetLocalpart().toString(), message.getTo().maybeGetLocalpart().toString(), message.getBody(), MessageTypes.TEXT, actualSqlDate, false);
                localMessageHistoryDatabase.addNewIncomingMessage(incomingUserMessage);
                localMessageHistoryDatabase.close();
                notifyNewMessage(message);
                Intent i = new Intent("com.bye.heyyou.heyyou.NEW_MESSAGE");
                sendBroadcast(i);
            }
        }
    }

    public boolean isChatWithUserOpen(String opponentUserId) {
        SharedPreferences settings = getSharedPreferences("active chat", MODE_PRIVATE);
        return settings.getString("ChatOpenWithUser", "").equals(opponentUserId);
    }

    private void notifyNewMessage(Message message) {
        if (!isChatWithUserOpen(message.getFrom().maybeGetLocalpart().toString())) {
            messageNotificationManager.increaseNumberNewMessages();
            messageNotificationManager.showNewMessage(message);
        }
    }

    private class Login implements Callable<Boolean> {
        @Override
        public Boolean call() {
            Log.d("XMPP", "login with Username " + getUserID());
            try {
                if (conn.isConnected()) {
                    try {
                        conn.login();
                    } catch (InterruptedException e) {
                        Log.e("XMPP", "login interrupted");
                    }
                    Log.d("XMPP", "Logged in");
                    setNewMessageListener();
                } else {
                    Log.e("XMPP", "not connected to " + getDbAddress() + ", no login");
                }
            } catch (SmackException.ConnectionException e) {
                Log.e("XMPP Exception1", e.getMessage());
            } catch (SmackException e) {
                Log.e("XMPP Exception2", e.getMessage());
                return false;
            } catch (IOException e) {
                Log.e("XMPP Exception3", e.getMessage());
            } catch (XMPPException e) {
                if (e.getMessage().equals("SASLError using SCRAM-SHA-1: bad-auth")) {
                    conn.disconnect();
                    return false;
                }
            }
            return conn.isAuthenticated();
        }
    }

    private class Register implements Callable<Boolean> {
        @Override
        public Boolean call() {
            Log.d("XMPP", "register with Username " + getUserID());
            if (conn.isConnected()) {
                try {
                    AccountManager accountManager = AccountManager.getInstance(conn);
                    accountManager.createAccount(getUserID(), getPassword());
                } catch (XMPPException.XMPPErrorException e) {
                    e.printStackTrace();
                    return false;
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    return false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                } catch (SmackException.NoResponseException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                Log.e("XMPP", "not connected to " + getDbAddress() + ", no register");
            }
            return true;
        }
    }


    /**
     * connects to the XMPP Server. After a successful connection a new Message Listener will be set.
     */
    private class Setup implements Runnable {
        @Override
        public void run() {
            XMPPTCPConnection.setUseStreamManagementDefault(true);
            XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
            builder.setHost(getDbAddress());
            builder.setPort(5222);
            try {
                builder.setServiceName(JidCreate.domainBareFrom(getDbAddress()));
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
            builder.setResource("HeyYouApp");
            builder.setUsernameAndPassword(getUserID(), getPassword());
            if (conn != null && conn.isConnected()) {
                conn.disconnect();
            }
            conn = new XMPPTCPConnection(builder.build());
        }
    }

    /**
     * connects to the XMPP Server. After a successful connection a new Message Listener will be set.
     */
    private class Connect implements Runnable {
        @Override
        public void run() {
            try {
                if (conn != null && conn.isConnected()) {
                    conn.disconnect();
                }
                if (conn != null) {
                    try {
                        conn.connect();
                    } catch (InterruptedException e) {
                        Log.e("XMPP", "login interrupted");
                    }
                }
                ReconnectionManager.getInstanceFor(conn).enableAutomaticReconnection();

            } catch (SmackException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XMPPException e) {
                e.printStackTrace();
            }
        }
    }


    private class SendMessage implements Runnable {
        OutgoingUserMessage newMessage;

        SendMessage(OutgoingUserMessage newMessage) {
            this.newMessage = newMessage;
        }

        @Override
        public void run() {
            if (chatmanager == null) {
                chatmanager = ChatManager.getInstanceFor(conn);
            }
            Chat newChat = null;
            try {
                newChat = chatmanager.createChat((JidWithLocalpart) JidCreate.from(newMessage.getToUserId(), getDbAddress(), ""), new newUserMessageListener());
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }

            if (newMessage.getMessageType() == MessageTypes.TEXT) {

                try {
                    if (newMessage != null && newMessage.getContent() != null) {
                        newChat.sendMessage(newMessage.getContent());
                        LocalMessageHistoryDatabase localMessageHistoryDatabase = new LocalMessageHistoryDatabase(getBaseContext());
                        localMessageHistoryDatabase.markAsSent(newMessage.getMessageId());
                        localMessageHistoryDatabase.close();
                    }

                } catch (InterruptedException e) {
                    Log.e("XMPP", "send Message interrupted");
                } catch (SmackException.NotConnectedException e) {
                    Log.e("XMPP Service", "Send Message failed not connected");
                }
            }
        }
    }

    public class XMPPServiceBinder extends Binder {
        public void sendMessage(OutgoingUserMessage newMessage) {
            xmppConnectionThreads.submit(new SendMessage(newMessage));
        }

        public boolean setUsernamePasswordAndLogin(String username, String password) {
            boolean mChanged = true;
            boolean mSuccessful = false;
            if (!password.equals(getPassword())) {
                setPassword(password);
                mChanged = true;
            }

            if (!username.equals(getUserID())) {
                setUserID(username);
                mChanged = true;
            }
            if (mChanged) {
                xmppConnectionThreads.submit(new Setup());
            }
            try {
                xmppConnectionThreads.submit(new Connect());
                mSuccessful = xmppConnectionThreads.submit(new Login()).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return mSuccessful;
        }

        public boolean setUsernamePasswordAndRegister(String username, String password) {
            boolean mSuccessful = false;
            if (!password.equals(getPassword())) {
                setPassword(password);
            }
            if (!username.equals(getUserID())) {
                setUserID(username);
            }
            xmppConnectionThreads.submit(new Setup());
            xmppConnectionThreads.submit(new Connect());
            try {
                mSuccessful = xmppConnectionThreads.submit(new Register()).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return mSuccessful;
        }
    }

}
