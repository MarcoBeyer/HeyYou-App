package com.bye.heyyou.heyyou.database;

import android.test.ActivityInstrumentationTestCase2;

import com.bye.heyyou.heyyou.MainActivity;
import com.bye.heyyou.heyyou.user.LocalUser;

import java.util.List;

public class LocationExternalDatabaseTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private LocationExternalDatabase db;

    public LocationExternalDatabaseTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        db = new LocationExternalDatabase("usertestalex", "http://db.heyyouapp.net");
        db.sendNewLocation(13.412, 52.521825, 1);
    }

    public void testSendLocation() throws InterruptedException {
        Boolean testUserFound = false;
        db = new LocationExternalDatabase("usertest1", "http://db.heyyouapp.net");
        db.sendNewLocation(13.412, 52.521825, 1);
        List<LocalUser> localUsers = db.getLocalUsers();
        assertTrue(localUsers.size() >= 1);
        for (LocalUser localUser : localUsers) {
            if (localUser.getUserID().equals("usertestalex")) {
                testUserFound = true;
            }
            if (localUser.getUserID().equals("usertest1")) {
                fail("user should not find himself");
            }
        }
        if (!testUserFound) {
            fail("local Test User not found!");
        }
    }
}
