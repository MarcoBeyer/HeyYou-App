package com.bye.heyyou.heyyou.service.location;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.bye.heyyou.heyyou.location.UserLocationManager;
import com.bye.heyyou.heyyou.user.LocalUser;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class LocationService extends Service implements Observer {

    private UserLocationManager locationManager;
    private final IBinder locationServiceBinder = new LocationServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        locationManager.setLocationSendInterval(5000);
        return locationServiceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        super.onUnbind(intent);
        locationManager.setLocationSendInterval(120000);
        return false;
    }

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

    private String getLocationSocketAddress() {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        return settings.getString("locationSocketUrl", "");
    }

    private void setLocationSocketAddress(String dbAddress) {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("locationSocketUrl", dbAddress);
        editor.apply();
    }

    private boolean getTrackUser() {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        return settings.getBoolean("trackUser", false);
    }

    private void setTrackUser(boolean trackUser) {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("trackUser", trackUser);
        editor.apply();
    }

    @Override
    public void update(Observable observable, Object data) {
        Intent i = new Intent("com.bye.heyyou.heyyou.NEW_LOCATION");
        sendBroadcast(i);
    }

    public class LocationServiceBinder extends Binder {
        public void stopGettingLocation() {
            locationManager.stopGettingLocation();
            setTrackUser(false);
        }

        public void startGettingLocation() {
            locationManager.startGettingLocation();
            setTrackUser(true);
        }

        public double getAccuracy(){
          return  locationManager.getAccuracy();
        }

        public List<LocalUser> getLocalUsers(){
            return locationManager.getLocalUsers();
        }

        public void setLocationSendInterval(int intervalInMs){
            locationManager.setLocationSendInterval(intervalInMs);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras;
        if (intent != null) {
            extras = intent.getExtras();
            if (extras != null && extras.size()!=0) {
                if (!getUserID().equals(extras.getString("userID"))) {
                    setUserID(extras.getString("userID"));
                }
                if (!getLocationSocketAddress().equals(extras.getString("locationSocketUrl"))) {
                    setLocationSocketAddress(extras.getString("locationSocketUrl"));
                }
            }

        }
        locationManager=new UserLocationManager(getBaseContext(),getUserID(), getLocationSocketAddress());
        if(getTrackUser()) {
            locationManager.startGettingLocation();
        }
        locationManager.addObserver(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.deleteObserver(this);
    }
}


