package com.bye.heyyou.heyyou.location;

import android.app.AlertDialog;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.bye.heyyou.heyyou.database.LocationExternalDatabase;
import com.bye.heyyou.heyyou.user.LocalUser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * The UserLocationManager connects to the GoogleAPIClient and informs the registered Observer about Location Changes.
 * It also questions the LocationExternalDatabase for users in the near of the new Location
 */
public class UserLocationManager extends Observable implements Observer, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private Context context;
    private int accuracy = -1;
    private LocationExternalDatabase locationExternalDatabase;
    private List<LocalUser> localUsers = new ArrayList<LocalUser>();
    private FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
    private GoogleApiClient googleApiClient;
    private int intervalInMs = 120000;
    private LocationRequest mLocationRequest = LocationRequest.create();
    ;

    public UserLocationManager(Context context, String userId, String databaseUrl) {
        this.context = context;
        locationExternalDatabase = new LocationExternalDatabase(userId, databaseUrl);
        locationExternalDatabase.addObserver(this);
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /**
     * starts the notification about location changes
     * Connects to the Google API Client
     */
    public void startGettingLocation() {
        googleApiClient.connect();
    }

    /**
     * Stops the notification about location changes.
     * Disconnects from the Google API Client
     */
    public void stopGettingLocation() {
        if (googleApiClient.isConnected()) {
            fusedLocationProviderApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    public List<LocalUser> getLocalUsers() {
        return localUsers;
    }

    /**
     * returns the accurancy of the last Location of the user.
     * It returns -1 if there is no last Location.
     *
     * @return the accurancy of the last Location of the user
     */
    public int getAccuracy() {
        return accuracy;
    }

    @Override
    public void update(Observable observable, Object data) {
        localUsers = locationExternalDatabase.getLocalUsers();
        setChanged();
        notifyObservers();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(intervalInMs);
        mLocationRequest.setFastestInterval(intervalInMs);
        fusedLocationProviderApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
    }

    public void setLocationSendInterval(int intervalInMs) {
        this.intervalInMs = intervalInMs;
        if (googleApiClient.isConnected()) {
            fusedLocationProviderApi.removeLocationUpdates(googleApiClient, this);
            mLocationRequest.setInterval(intervalInMs);
            mLocationRequest.setFastestInterval(intervalInMs);
            fusedLocationProviderApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        new AlertDialog.Builder(context)
                .setTitle("No Google Play Services")
                .setMessage("No Google Play Services found " + connectionResult.toString())
                .show();
    }

    @Override
    public void onLocationChanged(Location location) {
        UserLocation userlocation = new UserLocation(location.getLongitude(), location.getLatitude(), location.getAccuracy());
        accuracy = (int) Math.round(userlocation.getAccuracy());
        //send to server
        locationExternalDatabase.sendNewLocation(userlocation);
        localUsers = locationExternalDatabase.getLocalUsers();
        setChanged();
        Log.d("UserLocationManager", "new Location");
        notifyObservers();
    }
}
