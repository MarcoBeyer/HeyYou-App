package com.bye.heyyou.heyyou.service.xmpp;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.bye.heyyou.heyyou.message.OutgoingUserMessage;
import com.bye.heyyou.heyyou.notifications.MessageNotificationManager;
import com.bye.heyyou.heyyou.xmpp.connection.HeyYouConnection;

import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class XMPPService extends Service {
    private HeyYouConnection conn;
    private final IBinder xmppServiceBinder = new XMPPServiceBinder();
    private MessageNotificationManager messageNotificationManager;

    private String getUserID() {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        return settings.getString("userID", "");
    }

    private void setUserID(String userID) {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("userID", userID);
        editor.apply();
    }

    private String getPassword() {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        return settings.getString("password", "");
    }

    private void setPassword(String password) {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("password", password);
        editor.apply();
    }

    private String getDbAddress() {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        return settings.getString("dbAddress", "");
    }

    private void setDbAddress(String dbAddress) {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("dbAddress", dbAddress);
        editor.apply();
    }

    @Override
    public void onCreate() {
        messageNotificationManager = new MessageNotificationManager(this);
        setupConnection();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras;
        boolean mChanged = false;
        if (intent != null) {
            extras = intent.getExtras();
            if (extras != null) {
                if (!getPassword().equals(extras.getString("password"))) {
                    setPassword(extras.getString("password"));
                    mChanged = true;
                }
                if (!getUserID().equals(extras.getString("userID"))) {
                    setUserID(extras.getString("userID"));
                    mChanged = true;
                }
                if (!getDbAddress().equals(extras.getString("dbAddress"))) {
                    setDbAddress(extras.getString("dbAddress"));
                    mChanged = true;
                }
            }
        }

        if(getPassword().equals("") || getUserID().equals("") || getDbAddress().equals("")){
            return START_STICKY;
        }

        Log.d("Service", "Service started");
        if (mChanged) {
            setupConnection();
            conn.login();
        }
        else {
            Log.d("XMPP", "Connect and login");
            conn.login();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        conn.close();
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

    private void setupConnection() {
        if(conn!=null) {
            conn.close();
        }
        if(getPassword().equals("") || getUserID().equals("") || getDbAddress().equals("")){
            return;
        }

        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
        builder.setHost(getDbAddress());
        builder.setPort(5222);
        builder.setSendPresence(true);
        try {
            builder.setXmppDomain(JidCreate.domainBareFrom(getDbAddress()));
            builder.setResource("HeyYouApp");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return;
        }
        builder.setUsernameAndPassword(getUserID().toLowerCase(), getPassword());
        conn = new HeyYouConnection(getBaseContext(), builder.build());
    }


    public class XMPPServiceBinder extends Binder {
        public void sendMessage(OutgoingUserMessage newMessage) {
            conn.sendMessage(newMessage);
        }

        public boolean setUsernamePasswordAndLogin(String username, String password) {
            boolean mChanged = false;
            boolean mSuccessful;
            if (!password.equals(getPassword())) {
                setPassword(password);
                mChanged = true;
            }

            if (!username.equals(getUserID())) {
                setUserID(username);
                mChanged = true;
            }
            if (mChanged) {
                setupConnection();
            }
            mSuccessful = conn.login();
            return mSuccessful;
        }

        public boolean setUsernamePasswordAndRegister(String username, String password) {
            boolean mSuccessful;
            if (!password.equals(getPassword())) {
                setPassword(password);
            }
            if (!username.equals(getUserID())) {
                setUserID(username);
            }
            setupConnection();
            mSuccessful = conn.register();
            return mSuccessful;
        }
    }

}
