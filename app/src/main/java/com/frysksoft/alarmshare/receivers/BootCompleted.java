package com.frysksoft.alarmshare.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.frysksoft.alarmshare.config.Logging;
import com.frysksoft.alarmshare.scheduler.SunriseScheduler;

public class BootCompleted extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Make sure the right intent was provided
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Log phone startup
            Log.d(Logging.TAG, "Boot completed");

            // Reschedule sunrise alarm (without toast)
            SunriseScheduler.rescheduleSunriseAlarm(context, false);
        }
    }
}
