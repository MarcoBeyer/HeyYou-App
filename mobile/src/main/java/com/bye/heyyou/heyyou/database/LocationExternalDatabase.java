package com.bye.heyyou.heyyou.database;

import android.os.AsyncTask;
import android.util.Log;

import com.bye.heyyou.heyyou.location.UserLocation;
import com.bye.heyyou.heyyou.user.LocalUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class LocationExternalDatabase extends ExternalDatabase {
    private String locationSocketURL;
    private String myUserID;
    private List<LocalUser>localUsers= new ArrayList<>();
    private GetLocalUsersTask getLocalUsersTask=new GetLocalUsersTask();
    private Socket socket;

    public LocationExternalDatabase(String myUserID,String locationSocketURL) {
        this.myUserID = myUserID;
        this.locationSocketURL = locationSocketURL;
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
        private List<LocalUser> getLocalUsers(UserLocation userLocation) throws IOException {
            Log.d("LocationExternalDB", "new Location sent to " + locationSocketURL);
            String ip = locationSocketURL;  //localhost
            int port = 8489;

            if(socket==null || !socket.isConnected() || socket.isClosed()) {
                socket = new Socket(ip, port);  //verbindet sich mit Server
                socket.setKeepAlive(true);
            }
            sendLocation(socket, userLocation);
            List<LocalUser> localUsers = readResponse(socket);
            socket.close();
            return localUsers;
        }

        private void sendLocation(Socket socket, UserLocation userLocation) throws IOException {
            JSONObject location = new JSONObject();
            String locationJSON = "";
            try {
                location.put("user_id",myUserID);
                location.put("longitude",userLocation.getLongitude());
                location.put("latitude", userLocation.getLatitude());
                location.put("accuracy",userLocation.getAccuracy());
                locationJSON = location.toString();
                Log.d("LocationExternal","sending: "+locationJSON);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            PrintWriter printWriter =
                    new PrintWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream()));
            printWriter.print(locationJSON);
            printWriter.flush();
        }

        private List<LocalUser> readResponse(Socket socket) throws IOException {
            List<LocalUser>localUsers= new ArrayList<>();
            BufferedReader bufferedReader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()));
            char[] buffer = new char[100];
            //blockiert bis Nachricht empfangen
            int anzahlZeichen = bufferedReader.read(buffer, 0, 100);
            String nachricht = new String(buffer, 0, anzahlZeichen);
            Log.d("LocationExternalDB", "server answers:"+nachricht);
            JSONArray localUsersArray;
            try {
                localUsersArray = new JSONArray(nachricht);
                for (int i = 0; i < localUsersArray.length(); i++) {
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
            } catch (IOException e) {
                e.printStackTrace();
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
