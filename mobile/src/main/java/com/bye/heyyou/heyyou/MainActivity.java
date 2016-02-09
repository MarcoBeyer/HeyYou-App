package com.bye.heyyou.heyyou;


import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.bye.heyyou.heyyou.fragments.chat.ChatOverviewFragment;
import com.bye.heyyou.heyyou.fragments.location.LocalUserFragment;


public class MainActivity extends FragmentActivity {
    private Intent chatIntent;

    private String myUserID;
    private String locationServerURL = "heyyouapp.net";
    private String userMessageServerUrl = "chat.heyyouapp.net";
    private String password;
    private ChatOverviewFragment chatOverview;
    private LocalUserFragment localUserFragment;


    @Override
    protected void onStart() {
        super.onStart();
        if(!getSharedPreferences("user credentials",MODE_PRIVATE).contains("userId")||!getSharedPreferences("user credentials",MODE_PRIVATE).contains("password")){
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
        getSharedPreferences("user credentials",MODE_PRIVATE).edit().putString("userMessageServerUrl",userMessageServerUrl)
                .putString("locationSocketUrl", locationServerURL).apply();
        myUserID = getSharedPreferences("user credentials",MODE_PRIVATE).getString("userId", "");
        password = getSharedPreferences("user credentials",MODE_PRIVATE).getString("password", "");
        setContentView(R.layout.activity_main);

        chatIntent= new Intent(this,ChatActivity.class);
        if(savedInstanceState == null) {
            chatOverview =ChatOverviewFragment.newInstance(myUserID,password,userMessageServerUrl);
            localUserFragment = LocalUserFragment.newInstance(myUserID,locationServerURL);
            getFragmentManager().beginTransaction()
                    .add(R.id.localSearchViewFragmentContainer, localUserFragment)
                    .add(R.id.availableChatsFragmentContainer, chatOverview)
                    .commit();
        }
        else{
            chatOverview=ChatOverviewFragment.newInstance(myUserID,password,userMessageServerUrl);
            localUserFragment = LocalUserFragment.newInstance(myUserID,locationServerURL);
            getFragmentManager().beginTransaction()
                    .replace(R.id.availableChatsFragmentContainer, chatOverview)
                    .replace(R.id.localSearchViewFragmentContainer,localUserFragment )
                    .commit();
        }
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
                myUserID = getSharedPreferences("user credentials",MODE_PRIVATE).getString("userId", "user123456");
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
