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
        return settings.getString("userID", null);
    }

    private void setUserID(String userID) {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("userID", userID);
        editor.apply();
    }

    private String getDbAddress() {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        return settings.getString("locationDbAddress", null);
    }

    private void setDbAddress(String dbAddress) {
        SharedPreferences settings = getSharedPreferences("user credentials", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("locationDbAddress", dbAddress);
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
            if (extras != null) {

                if (extras.getString("userID") != null) {
                    if (!extras.getString("userID").equals(getUserID())) {
                        setUserID(extras.getString("userID"));
                    }
                }

                if (extras.getString("dbAddress") != null) {
                    if (!extras.getString("dbAddress").equals(getDbAddress())) {
                        setDbAddress(extras.getString("dbAddress"));
                    }
                }
            }

        }
        locationManager=new UserLocationManager(getBaseContext(),getUserID(),getDbAddress());
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


