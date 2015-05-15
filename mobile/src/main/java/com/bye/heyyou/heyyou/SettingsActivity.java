package com.bye.heyyou.heyyou;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;

import java.util.List;

public class SettingsActivity extends PreferenceActivity {
    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This fragment shows the preferences for the first header.
     */
    public static class GeneralPrefs1Fragment extends PreferenceFragment {
        SharedPreferences.OnSharedPreferenceChangeListener listener;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            SharedPreferences prefs = this.getPreferenceManager().getSharedPreferences();
            listener = new SharedPreferences.OnSharedPreferenceChangeListener() {

                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if(key.equals("displayName")){
                        Log.d("Preferences","DisplayName changed");
                    }
                }
            };
            prefs.registerOnSharedPreferenceChangeListener(listener);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.pref_general);
        }
    }

    protected boolean isValidFragment(String fragmentName) {
        return GeneralPrefs1Fragment.class.getName().equals(fragmentName);
    }

}