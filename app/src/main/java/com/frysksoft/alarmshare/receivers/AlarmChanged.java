package com.frysksoft.alarmshare.receivers;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.frysksoft.alarmshare.config.Logging;
import com.frysksoft.alarmshare.scheduler.SunriseScheduler;

public class AlarmChanged extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Make sure the right intent was provided
        if (intent.getAction().equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)) {
            // Log alarm changed
            Log.d(Logging.TAG, "Alarm clock changed");

            // Reschedule sunrise alarm (without toast)
            SunriseScheduler.rescheduleSunriseAlarm(context, false);
        }
    }
}
