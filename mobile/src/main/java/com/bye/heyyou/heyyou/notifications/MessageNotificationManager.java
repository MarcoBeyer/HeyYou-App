package com.bye.heyyou.heyyou.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;

import com.bye.heyyou.heyyou.MainActivity;
import com.bye.heyyou.heyyou.R;

import org.jivesoftware.smack.packet.Message;

public class MessageNotificationManager {
    private Context context;

    public MessageNotificationManager(Context context){
     this.context=context;
     }

    private int getNumberNewMessages() {
        SharedPreferences settings = context.getSharedPreferences("messages", Context.MODE_PRIVATE);
        return settings.getInt("numberNewMessages", 0);
    }

    public void resetNumberNewMessages() {
        SharedPreferences settings = context.getSharedPreferences("messages", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("numberNewMessages", 0);
        editor.apply();
    }

    public void increaseNumberNewMessages() {
        SharedPreferences settings = context.getSharedPreferences("messages", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("numberNewMessages", settings.getInt("numberNewMessages", 0) + 1);
        editor.apply();
    }

    public void deleteNotification() {
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(1);
    }

    public void showNewMessage(Message message) {
        long[] pattern = {0, 500};
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(message.getFrom().getLocalpartOrNull().toString())
                        .setVibrate(pattern)
                        .setNumber(getNumberNewMessages());
        if(getNumberNewMessages()==1){
            mBuilder.setContentText(message.getBody());
        }
        else{
            mBuilder.setContentText(getNumberNewMessages()+" "+ context.getString(R.string.number_new_messages));
        }
        Intent resultIntent = new Intent(context,MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(1, mBuilder.build());
    }
}
