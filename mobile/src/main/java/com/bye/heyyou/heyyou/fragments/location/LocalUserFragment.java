package com.bye.heyyou.heyyou.fragments.location;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.bye.heyyou.heyyou.R;
import com.bye.heyyou.heyyou.service.location.LocationServiceConnection;
import com.bye.heyyou.heyyou.user.LocalUser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class LocalUserFragment extends Fragment implements Observer {
    private static final String username_arg1 = "username";
    private static final String locationDb_arg2 = "locationDb";

    private String myUserID;
    private String locationServerURL;
    private View localUserView;

    private LocationServiceConnection userLocationManager;
    private LayoutInflater inflater;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param username Parameter 1.
     * @param locationDB Parameter 2.
     * @return A new instance of fragment LocalUserFragment.
     */
    public static LocalUserFragment newInstance(String username, String locationDB) {
        LocalUserFragment fragment = new LocalUserFragment();
        Bundle args = new Bundle();
        args.putString(username_arg1, username);
        args.putString(locationDb_arg2, locationDB);
        fragment.setArguments(args);
        return fragment;
    }

    public LocalUserFragment() {
        // Required empty public constructor
    }

    public void onLocalSearchSwitch(boolean isChecked) {
        LinearLayout localSearchResultListView = (LinearLayout) localUserView.findViewById(R.id.localSearchViewContainer);
        if (isChecked) {
            //check if Location Tracking is on
            if (!checkForLocationModeOn()||!checkForGooglePlayServices()) {
                userLocationManager.stopGettingLocation();
                ((Switch) localUserView.findViewById(R.id.localSearchSwitch)).setChecked(false);
            }else{
                startDisplayingLocalUsers();
            }
        } else {
            stopDisplayingLocalUsers();
        }
    }

    private void displayLocalUser() {
        Switch accuracyDisplay = (Switch) localUserView.findViewById(R.id.localSearchSwitch);
        if (userLocationManager.getAccuracy() != -1 && accuracyDisplay.isChecked()) {
            accuracyDisplay.setText(getString(R.string.localsearch_switch) + "\n" + Math.round(userLocationManager.getAccuracy()) + getString(R.string.metersAccuracy));
        }
        LinearLayout localUserDisplay = (LinearLayout) localUserView.findViewById(R.id.localSearchViewContainer);
        localUserDisplay.removeAllViews();
        List<LocalUser> localUsers = userLocationManager.getLocalUsers();
        for (LocalUser localUser : localUsers) {
            View localUserRow = inflater.inflate(R.layout.local_user_row, localUserDisplay, false);
            localUserRow.setTag(localUser.getUserID());
            TextView opponentUserIdTextView = (TextView) localUserRow.findViewById(R.id.opponentUserID);
            opponentUserIdTextView.setText(localUser.getUserID());
            TextView locationDetail = (TextView) localUserRow.findViewById(R.id.detail);
            locationDetail.setText(String.valueOf(localUser.getDistanceInMeters()));
            localUserDisplay.addView(localUserRow);
        }
    }

    private boolean getTrackUser() {
        SharedPreferences settings = getActivity().getSharedPreferences("user credentials", Context.MODE_PRIVATE);
        return settings.getBoolean("trackUser", false);
    }

    public void startDisplayingLocalUsers(){
        LinearLayout localSearchResultListView = (LinearLayout) localUserView.findViewById(R.id.localSearchViewContainer);
        localSearchResultListView.setVisibility(View.VISIBLE);
        userLocationManager.startGettingLocation();
    }

    public void stopDisplayingLocalUsers(){
        LinearLayout localSearchResultListView = (LinearLayout) localUserView.findViewById(R.id.localSearchViewContainer);
        Switch accuracyDisplay = (Switch) localUserView.findViewById(R.id.localSearchSwitch);
        accuracyDisplay.setText(getString(R.string.localsearch_switch));
        localSearchResultListView.removeAllViews();
        localSearchResultListView.setVisibility(View.GONE);
        userLocationManager.stopGettingLocation();
    }

    public boolean checkForLocationModeOn(){
        try {
            if(Settings.Secure.getInt(getActivity().getContentResolver(), Settings.Secure.LOCATION_MODE.toString())==Settings.Secure.LOCATION_MODE_OFF){
                new AlertDialog.Builder(getActivity())
                        .setTitle("Please activate Location")
                        .setMessage("Please activate Location in Settings")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .show();
                return false;
            }
        } catch (Settings.SettingNotFoundException e) {
            //TODO alternatives
        }
        return true;
    }

    public boolean checkForGooglePlayServices(){
        if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getActivity())==ConnectionResult.SUCCESS){
            return true;
        }
        GoogleApiAvailability.getInstance().getErrorDialog(getActivity(),GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getActivity()),5);
        new AlertDialog.Builder(getActivity())
                .setTitle("No Google Play Services")
                .setMessage("No Google Play Services found or not active"+ GoogleApiAvailability.getInstance().getErrorString(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getActivity())))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
        return false;
    }

    @Override
    public void update(Observable observable, Object data) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayLocalUser();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            myUserID = getArguments().getString(username_arg1);
            locationServerURL = getArguments().getString(locationDb_arg2);
        }
        userLocationManager = new LocationServiceConnection(getActivity(),myUserID,locationServerURL);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(checkForGooglePlayServices()&&checkForLocationModeOn()) {
            userLocationManager.register();
            userLocationManager.setLocationSendInterval(5000);
        }else{
            userLocationManager.stopGettingLocation();
            ((Switch) localUserView.findViewById(R.id.localSearchSwitch)).setChecked(false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        userLocationManager.setLocationSendInterval(120000);
        userLocationManager.unregister();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        userLocationManager.unregister();
        userLocationManager.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        userLocationManager.addObserver(this);
        this.inflater = inflater;
        localUserView = inflater.inflate(R.layout.fragment_local_user, container, false);
        ((Switch) localUserView.findViewById(R.id.localSearchSwitch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton switchView, boolean isChecked) {
                onLocalSearchSwitch(isChecked);
            }
        });
        if (getTrackUser()){
            ((Switch) localUserView.findViewById(R.id.localSearchSwitch)).setChecked(true);
        } else
        {
            ((Switch) localUserView.findViewById(R.id.localSearchSwitch)).setChecked(false);
        }
        return localUserView;
    }



}
