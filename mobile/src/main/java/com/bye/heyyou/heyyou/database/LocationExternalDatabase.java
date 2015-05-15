package com.bye.heyyou.heyyou.database;

import android.os.AsyncTask;
import android.util.Log;

import com.bye.heyyou.heyyou.exceptions.GetLocalUsersFailedException;
import com.bye.heyyou.heyyou.location.UserLocation;
import com.bye.heyyou.heyyou.user.LocalUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocationExternalDatabase extends ExternalDatabase {
    private String databaseURL;
    private String myUserID;
    private List<LocalUser>localUsers= new ArrayList<>();
    private GetLocalUsersTask getLocalUsersTask=new GetLocalUsersTask();

    public LocationExternalDatabase(String myUserID,String databaseURL) {
        this.myUserID = myUserID;
        this.databaseURL = databaseURL;
    }

    public List<LocalUser> getLocalUsers() {
            return  localUsers;
    }

    /**
     * Sends a Location to the Database which will be stored
     *
     * @param latitude  The Latitude of the User
     * @param longitude The Longitude of the User
     * @param accuracy  The Radius from the Latitude-Longitude-Point
     */
    public void sendNewLocation( double longitude, double latitude, double accuracy) {
        sendNewLocation(new UserLocation(longitude, latitude, accuracy));
    }

    public void sendNewLocation(UserLocation userLocation) {
        //no second Task should be started until the first has finished
        if(getLocalUsersTask.getStatus().equals(AsyncTask.Status.FINISHED)||getLocalUsersTask.getStatus().equals(AsyncTask.Status.PENDING)) {
            getLocalUsersTask = new GetLocalUsersTask();
            getLocalUsersTask.execute(userLocation);
        }
    }



    private class GetLocalUsersTask extends AsyncTask<UserLocation, Void, List<LocalUser>> {

        private List<LocalUser> getLocalUsers(UserLocation userLocation) throws GetLocalUsersFailedException, IOException {
            List<LocalUser> localUsers = new ArrayList<>();
            Connection connection = Jsoup.connect(databaseURL + "/getLocalUsers.php");
            Log.d("LocationExternalDB","new Location sent to "+ databaseURL);
            Document doc = connection
                    .data("user_id", myUserID)
                    .data("longitude", String.valueOf(userLocation.getLongitude()))
                    .data("latitude", String.valueOf(userLocation.getLatitude()))
                    .data("accuracy", String.valueOf(userLocation.getAccuracy()))
                    .post();
            if (connection.response().statusCode() != 200) {
                throw new GetLocalUsersFailedException(connection.response().statusCode());
            }
            String content = doc.body().text();
            Log.d("LocationExternalDB", content);
            JSONArray localUsersArray;
            try {
                localUsersArray = new JSONArray(content);
            for (int i = 0; i<localUsersArray.length();i++){
                JSONArray localUserArray = localUsersArray.getJSONArray(i);
                localUsers.add(new LocalUser(localUserArray.get(0).toString(), Double.valueOf(localUserArray.get(1).toString()), Double.valueOf(localUserArray.get(2).toString())));
            }
            } catch (JSONException e) {
            e.printStackTrace();
        }
            return localUsers;
        }

        @Override
        protected List<LocalUser> doInBackground(UserLocation... params) {
            List<LocalUser>localUsers=new ArrayList<>();
            try {
                localUsers=getLocalUsers(params[0]);
            } catch (GetLocalUsersFailedException e) {
                return localUsers;
            } catch (IOException e) {
                return localUsers;
            }
            return localUsers;
        }

        @Override
        protected void onPostExecute(List<LocalUser> newLocalUsers){
            localUsers = newLocalUsers;
            setChanged();
            notifyObservers();
        }

    }
}
