package com.bye.heyyou.heyyou.service.location;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.bye.heyyou.heyyou.user.LocalUser;

import java.util.List;
import java.util.Observable;


/**
 * The XMPP Service Connection connects to the running XMPPService or starts him.
 * It gives you the possibility to send messages and change login credentials.
 */

public class LocationServiceConnection extends Observable {
    private NewLocationReceiver newLocationReceiver;
    private Context context;
    private LocationService.LocationServiceBinder locationServiceBinder;
    private boolean mBound = true;
    private Intent newService;
    private boolean mBroadcastRegistered;

    public LocationServiceConnection(final Context context, final String username, final String locationSocketAddress) {
        this.context=context;
        newService = new Intent(context, LocationService.class);
        newService.putExtra("userID",username);
        newService.putExtra("locationSocketUrl",locationSocketAddress);
        context.startService(newService);
        context.bindService(newService, mConnection, Context.BIND_AUTO_CREATE);
        register();

        }

    public void unregister(){
        if (mBroadcastRegistered) {
            context.unregisterReceiver(newLocationReceiver);
            mBroadcastRegistered=false;
        }
    }

    public void register(){
        if (!mBroadcastRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.bye.heyyou.heyyou.NEW_LOCATION");
            newLocationReceiver = new NewLocationReceiver();
            context.registerReceiver(newLocationReceiver, intentFilter);
            mBroadcastRegistered = true;
        }
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

    public void startGettingLocation(){
        if(locationServiceBinder!=null) {
            locationServiceBinder.startGettingLocation();
        }
    }

    public void stopGettingLocation(){
        if(locationServiceBinder!=null) {
            locationServiceBinder.stopGettingLocation();
        }
    }

    public List<LocalUser> getLocalUsers(){
        return locationServiceBinder.getLocalUsers();
    }

    public double getAccuracy(){
        return locationServiceBinder.getAccuracy();
    }

    public void setLocationSendInterval(int intervalInMs){
        if(locationServiceBinder!=null) {
            locationServiceBinder.setLocationSendInterval(intervalInMs);
        }
    }

    public class NewLocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("locationservice","received intent "+intent.toString());
            setChanged();
            notifyObservers();
        }

    }
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName className,IBinder service) {
        Log.d("LocationDatabase","Connected to Service");
        locationServiceBinder = (LocationService.LocationServiceBinder) service;
        mBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
