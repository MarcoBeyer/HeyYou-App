package com.bye.heyyou.heyyou.service.xmpp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.bye.heyyou.heyyou.message.OutgoingUserMessage;


/**
 * The XMPP Service Connection connects to the running XMPPService or starts him.
 * It gives you the possibility to send messages and change login credentials.
 */

public class XMPPServiceConnection {
    private Context context;
    private XMPPService.XMPPServiceBinder xmppServiceBinder;
    private boolean mBound;
    private Intent newService;

    public XMPPServiceConnection(final Context context, final String username, final String password, final String dbAddress) {
        this.context=context;
        newService = new Intent(context, XMPPService.class);
        newService.putExtra("userID",username);
        newService.putExtra("password",password);
        newService.putExtra("dbAddress",dbAddress);
        context.startService(newService);
        context.bindService(newService,mConnection,Context.BIND_AUTO_CREATE);
        }
    public XMPPServiceConnection(final Context context, final String dbAddress) {
        this.context=context;
        newService = new Intent(context, XMPPService.class);
        newService.putExtra("dbAddress",dbAddress);
        context.startService(newService);
        context.bindService(newService,mConnection,Context.BIND_AUTO_CREATE);
    }


    public void close(){
        if (mBound) {
            context.unbindService(mConnection);
            mBound = false;
        }
    }

    public void open(){
        if (!mBound) {
            context.bindService(newService,mConnection,Context.BIND_AUTO_CREATE);
        }
    }

    public boolean setUsernamePasswordAndLogin(String username, String password){
       return xmppServiceBinder.setUsernamePasswordAndLogin(username, password);
    }

    public boolean setUsernamePasswordAndRegister(String username, String password){
        return xmppServiceBinder.setUsernamePasswordAndRegister(username, password);
    }

    public void sendMessage(OutgoingUserMessage newMessage){
        xmppServiceBinder.sendMessage(newMessage);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName className,IBinder service) {
        Log.d("XMPPDatabase","Connected to Service");
        xmppServiceBinder = (XMPPService.XMPPServiceBinder) service;
        mBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
