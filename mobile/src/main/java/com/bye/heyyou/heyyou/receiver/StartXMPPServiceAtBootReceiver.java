package com.bye.heyyou.heyyou.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bye.heyyou.heyyou.service.xmpp.XMPPService;

public class StartXMPPServiceAtBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, XMPPService.class);
            context.startService(serviceIntent);
        }
    }
}