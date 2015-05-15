package com.bye.heyyou.heyyou.message;

import java.sql.Timestamp;
import java.util.Comparator;

public class UserMessageTimeComparator implements Comparator<UserMessage> {
    public int compare(UserMessage um1, UserMessage um2) {
        Timestamp um1Date = um1.getSentTime();
        Timestamp um2Date = um2.getSentTime();
        return um1Date.compareTo(um2Date);
    }
}
