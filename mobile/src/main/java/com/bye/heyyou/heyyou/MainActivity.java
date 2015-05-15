package com.bye.heyyou.heyyou;


import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.bye.heyyou.heyyou.exceptions.ChatNotFoundException;
import com.bye.heyyou.heyyou.fragments.chat.ChatOverviewFragment;
import com.bye.heyyou.heyyou.fragments.location.LocalUserFragment;


public class MainActivity extends FragmentActivity {
    private Intent chatIntent;

    private String myUserID;
    private String locationServerURL = "http://db.heyyouapp.net";
    private String userMessageServerUrl = "chat.heyyouapp.net";
    private String password;
    private ChatOverviewFragment chatOverview;
    private LocalUserFragment localUserFragment;


    @Override
    protected void onStart() {
        super.onStart();
        if(!PreferenceManager.getDefaultSharedPreferences(this).contains("userId")||!PreferenceManager.getDefaultSharedPreferences(this).contains("password")){
            startActivityForResult(new Intent(this, LoginActivity.class), 2);
        }
    }


/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("userMessageServerUrl",userMessageServerUrl)
                .putString("userLocationServerUrl", locationServerURL).apply();
        myUserID = PreferenceManager.getDefaultSharedPreferences(this).getString("userId", "user123456");
        password = PreferenceManager.getDefaultSharedPreferences(this).getString("password", null);
        setContentView(R.layout.activity_main);

        chatOverview =ChatOverviewFragment.newInstance(myUserID,password,userMessageServerUrl);
        localUserFragment = LocalUserFragment.newInstance(myUserID,locationServerURL);
        chatIntent= new Intent(this,ChatActivity.class);
        getFragmentManager().beginTransaction()
                .add(R.id.localSearchViewFragmentContainer, localUserFragment)
                .add(R.id.availableChatsFragmentContainer, chatOverview)
                .commit();
    }

/*    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

    public void onLocalUserClick(View view) {
        Bundle bundle = new Bundle();
        bundle.putString("OpponentUserId", (String) view.getTag());
        chatIntent.putExtras(bundle);
        startActivity(chatIntent);
    }

    public void onChatClick(View view) {
       if(chatOverview!=null){
           chatOverview.onChatClick(view);
       }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       final int LOGIN_REQUEST=2;
        // Check which request we're responding to
        if (requestCode == LOGIN_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                myUserID = PreferenceManager.getDefaultSharedPreferences(this).getString("userId", "user123456");
                chatOverview=ChatOverviewFragment.newInstance(myUserID,password,userMessageServerUrl);
                localUserFragment = LocalUserFragment.newInstance(myUserID,locationServerURL);
                getFragmentManager().beginTransaction()
                        .replace(R.id.availableChatsFragmentContainer, chatOverview)
                        .replace(R.id.localSearchViewFragmentContainer,localUserFragment )
                        .commit();
            }
        }
    }
}
