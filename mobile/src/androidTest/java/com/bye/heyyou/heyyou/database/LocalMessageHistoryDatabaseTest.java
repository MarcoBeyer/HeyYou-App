package com.bye.heyyou.heyyou.database;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

import com.bye.heyyou.heyyou.MainActivity;
import com.bye.heyyou.heyyou.chat.LocalMessageHistoryDatabase;
import com.bye.heyyou.heyyou.exceptions.NoNewMessageException;
import com.bye.heyyou.heyyou.message.IncomingUserMessage;
import com.bye.heyyou.heyyou.message.MessageTypes;
import com.bye.heyyou.heyyou.message.UserMessage;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class LocalMessageHistoryDatabaseTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private Activity testActivity;
    private LocalMessageHistoryDatabase db;

    public LocalMessageHistoryDatabaseTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testActivity = getActivity();
        db = new LocalMessageHistoryDatabase(testActivity);
    }

    public void testGetMessages() {
        List<UserMessage> actual = new ArrayList<UserMessage>();
        db.deleteChatWithUser("usertest2");
        java.util.Date utilDate = new java.util.Date();
        Timestamp actualSqlDate = new Timestamp(utilDate.getTime());
        db.addNewIncomingMessage(new IncomingUserMessage(String.valueOf(db.generateNewMessageID()), "test1", "usertest2", "HeyYou", MessageTypes.TEXT, actualSqlDate, false));
        try {
            actual = db.getMessagesWithOppositeUser("usertest2");
        } catch (NoNewMessageException e) {
            fail("No New Message!");
        }
        assertEquals(1, actual.size());
    }

    public void testAddNewIncomingMessage() {
        db.deleteChatWithUser("usertest2");
        java.util.Date utilDate = new java.util.Date();
        Timestamp actualSqlDate = new Timestamp(utilDate.getTime());
        IncomingUserMessage message = new IncomingUserMessage(String.valueOf(db.generateNewMessageID()), "test1", "usertest2", "HeyYou", MessageTypes.TEXT, actualSqlDate, false);
        IncomingUserMessage message2 = new IncomingUserMessage(String.valueOf(db.generateNewMessageID()), "test1", "usertest2", "HeyYou2", MessageTypes.TEXT, actualSqlDate, false);
        db.addNewIncomingMessage(message);
        db.addNewIncomingMessage(message2);
        try {
            assertEquals(2, db.getMessagesWithOppositeUser("usertest2").size());
        } catch (NoNewMessageException e) {
            fail();
        }
    }


}