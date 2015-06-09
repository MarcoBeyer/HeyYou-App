package com.bye.heyyou.heyyou.xmpp.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.bye.heyyou.heyyou.database.LocalMessageHistoryDatabase;
import com.bye.heyyou.heyyou.message.MessageTypes;
import com.bye.heyyou.heyyou.message.OutgoingUserMessage;
import com.bye.heyyou.heyyou.xmpp.connection.listener.newUserMessageListener;

import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.JidWithLocalpart;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HeyYouConnection {

    static{
        XMPPTCPConnection.setUseStreamManagementDefault(true);
        XMPPTCPConnection.setUseStreamManagementResumptiodDefault(true);
    }

    private Context context;
    private XMPPTCPConnection conn;
    private ChatManager chatmanager;
    private ExecutorService xmppConnectionThreads;
    private boolean mBroadcastRegistered;
    private ConnectivityChangeReceiver connectivityChangeReceiver;
    private ConnectivityManager cm;
    private PingManager pm;
    private DomainBareJid host;
    private String userName;
    private String password;
    private ReconnectionManager rm;

    public HeyYouConnection(Context context,XMPPTCPConnectionConfiguration config){
        this.context=context;
        xmppConnectionThreads = Executors.newSingleThreadExecutor();
        xmppConnectionThreads.submit(new Setup(config));
        xmppConnectionThreads.submit(new Connect());
        userName = config.getUsername().toString();
        host = config.getServiceName();
        password = config.getPassword();
        registerBroadcastReceiver();
    }

    public void close(){
        conn.disconnect();
        unregister();
        xmppConnectionThreads.shutdown();
    }

    public void sendMessage(OutgoingUserMessage newMessage) {
        xmppConnectionThreads.submit(new SendMessage(newMessage));
    }

    public boolean login() {
        Log.d("XMPP","Login");
        try {
            return xmppConnectionThreads.submit(new Login()).get();

        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean register() {
        try {
            return xmppConnectionThreads.submit(new Register()).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }


    private class Setup implements Runnable {
        XMPPTCPConnectionConfiguration config;
        public Setup(XMPPTCPConnectionConfiguration config){
            this.config=config;
        }
        @Override
        public void run() {
            if (conn != null && conn.isConnected()) {
                conn.disconnect();
            }
            Log.d("XMPP","Setup connection");
            conn = new XMPPTCPConnection(config);
            pm = PingManager.getInstanceFor(conn);
            pm.setPingInterval(100000);
            pm.registerPingFailedListener(new HeyYouPingFailedListener());
            rm = ReconnectionManager.getInstanceFor(conn);
            rm.enableAutomaticReconnection();

        }

        private class HeyYouPingFailedListener implements PingFailedListener {
            @Override
            public void pingFailed() {
                conn.disconnect();
                try {
                    conn.connect();
                } catch (SmackException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void registerBroadcastReceiver(){
        if(!mBroadcastRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            connectivityChangeReceiver = new ConnectivityChangeReceiver();
            context.registerReceiver(connectivityChangeReceiver, intentFilter);
            mBroadcastRegistered = true;
        }
    }

    private void unregister(){
        if (mBroadcastRegistered) {
            context.unregisterReceiver(connectivityChangeReceiver);
            mBroadcastRegistered=false;
        }
    }


    private class ConnectivityChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if(activeNetwork!=null && activeNetwork.isConnected()){
                if(conn!=null) {
                    if (conn.isConnected() && conn.isAuthenticated()) {
                        xmppConnectionThreads.submit(new SendNotSentMessages());
                    } else {
                        xmppConnectionThreads.submit(new Connect());
                        xmppConnectionThreads.submit(new Login());
                        xmppConnectionThreads.submit(new SendNotSentMessages());
                    }
                }
            }else {
                if(conn!=null) {
                    conn.disconnect();
                }
            }
        }
    }

    private void setNewMessageListener() {
        chatmanager = ChatManager.getInstanceFor(conn);
        chatmanager.addChatListener(
                new ChatManagerListener() {
                    @Override
                    public void chatCreated(Chat chat, boolean createdLocally) {
                        if (!createdLocally && chat!=null)
                            chat.addMessageListener(new newUserMessageListener(context));
                    }
                });
    }


    private class Login implements Callable<Boolean> {
        @Override
        public Boolean call() {
            try {
                if (!conn.isConnected()) {
                    return false;
                }
                if(conn.isAuthenticated()){
                    return true;
                }
                try {
                    conn.login();
                    Log.d("XMPP", "Logged in at "+host);
                    setNewMessageListener();
                    Roster.getInstanceFor(conn);

                } catch (InterruptedException e) {
                    Log.e("XMPP", "login interrupted");
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
            Log.d("XMPP", "register with Username " + userName);
            if (!conn.isConnected()) {
                return false;
            }
            try {
                AccountManager accountManager = AccountManager.getInstance(conn);
                accountManager.createAccount(userName, password);
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
            return true;
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
                    return;
                }
                if (conn != null && cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnected()) {
                    try {
                        conn.setUseStreamManagement(true);
                        conn.setUseStreamManagementResumption(true);
                        conn.connect();
                        Log.d("SmResumption", "possible:"+conn.isSmResumptionPossible());
                        Log.d("SmResumption", "available:"+conn.isSmAvailable());
                        Log.d("SmResumption", "enabled:"+conn.isSmEnabled());
                        Log.d("SmResumption", "was resumed:"+conn.streamWasResumed());
                    } catch (InterruptedException e) {
                        Log.e("XMPP", "connect interrupted");
                    }
                }
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
            Chat newChat;
            try {
                newChat = chatmanager.createChat((JidWithLocalpart) JidCreate.from(newMessage.getToUserId(), host, ""), new newUserMessageListener(context));
            } catch (XmppStringprepException e) {
                e.printStackTrace();
                return;
            }

            if (newMessage.getMessageType() == MessageTypes.TEXT) {
                try {
                    if (newMessage != null && newMessage.getContent() != null) {
                        newChat.sendMessage(newMessage.getContent());
                        LocalMessageHistoryDatabase localMessageHistoryDatabase = new LocalMessageHistoryDatabase(context);
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

    private class SendNotSentMessages implements Runnable {
        @Override
        public void run() {
            if (chatmanager == null) {
                chatmanager = ChatManager.getInstanceFor(conn);
            }
            LocalMessageHistoryDatabase localMessageHistoryDatabase = new LocalMessageHistoryDatabase(context);
            for(OutgoingUserMessage newMessage : localMessageHistoryDatabase.getNotSentMessages()) {
                Chat newChat;
                try {
                    newChat = chatmanager.createChat((JidWithLocalpart) JidCreate.from(newMessage.getToUserId(), host, ""), new newUserMessageListener(context));
                } catch (XmppStringprepException e) {
                    e.printStackTrace();
                    return;
                }
                if (newMessage.getMessageType() == MessageTypes.TEXT) {
                    try {
                        if (newMessage.getContent() != null) {
                            newChat.sendMessage(newMessage.getContent());
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
    }
}
