package com.frysksoft.alarmshare.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.frysksoft.alarmshare.config.Logging;
import com.frysksoft.alarmshare.config.Notifications;
import com.frysksoft.alarmshare.utils.SingletonServices;

public class HideMoonlightReminder extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Log what we're doing
        Log.d(Logging.TAG, "Hiding moonlight reminder notification (if still displayed)");

        // Cancel moonlight reminder notification
        SingletonServices.getNotificationManager(context).cancel(Notifications.MOONLIGHT_REMINDER_NOTIFICATION_ID);
    }
}
