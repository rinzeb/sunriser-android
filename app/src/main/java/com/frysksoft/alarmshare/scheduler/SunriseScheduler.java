package com.frysksoft.alarmshare.scheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.frysksoft.alarmshare.alarms.SystemClock;
import com.frysksoft.alarmshare.config.Logging;
import com.frysksoft.alarmshare.config.Notifications;
import com.frysksoft.alarmshare.receivers.HideMoonlightReminder;
import com.frysksoft.alarmshare.receivers.ShowMoonlightReminder;
import com.frysksoft.alarmshare.services.SunriseService;
import com.frysksoft.alarmshare.utils.AppPreferences;
import com.frysksoft.alarmshare.utils.SingletonServices;
import com.frysksoft.alarmshare.utils.formatters.CountdownFormatter;

public class SunriseScheduler {
    public static void rescheduleSunriseAlarm(Context context, boolean showToast) {
        // Clear all previously-scheduled sunrise alarms
        SingletonServices.getAlarmManager(context).cancel(getSunriseAlarmPendingIntent(context));

        // Cancel any previously scheduled moonlight reminders
        SingletonServices.getAlarmManager(context).cancel(getMoonlightReminderPendingIntent(context));

        // Cancel any previously scheduled moonlight hide tasks
        SingletonServices.getAlarmManager(context).cancel(getHideReminderPendingIntent(context));

        // App disabled?
        if (!AppPreferences.isAppEnabled(context)) {
            // Don't reschedule any alarms
            return;
        }

        // Get current time (UTC)
        long now = System.currentTimeMillis();

        // Acquire next system alarm timestamp (in UTC)
        long nextAlarm = SystemClock.getNextAlarmTriggerTimestamp(context);

        // No alarm scheduled?
        if (nextAlarm == 0) {
            // Nothing to schedule, then
            return;
        }

        // Calculate when the sunrise alarm should commence (prior to the scheduled system alarm)
        long startSunriseTimestamp = nextAlarm - (AppPreferences.getSunriseHeadstartMinutes(context) * 60 * 1000);

        // Sunrise should have started already? (If next alarm is scheduled within the head-start time)
        if (startSunriseTimestamp < now) {
            // Don't schedule a sunrise alarm in the past
            return;
        }

        // Schedule the sunrise alarm by specifying the start time & pending intent to execute (the sunrise service)
        SingletonServices.getAlarmManager(context).setExact(AlarmManager.RTC_WAKEUP, startSunriseTimestamp, getSunriseAlarmPendingIntent(context));

        // Schedule a reminder to go to sleep
        scheduleMoonlightReminder(nextAlarm, startSunriseTimestamp, context);

        // Get a human-readable countdown message to display and log
        String countdownMessage = CountdownFormatter.getAlarmCountdownText(startSunriseTimestamp, context);

        // Alert the user (if invoked when the app is visible)
        if (showToast) {
            // Show a toast with countdown
            Toast.makeText(context, countdownMessage, Toast.LENGTH_LONG).show();
        }

        // Log the countdown
        Log.d(Logging.TAG, countdownMessage);
    }

    private static void scheduleMoonlightReminder(long nextAlarm, long startSunriseTimestamp, Context context) {
        // Cancel old moonlight reminder notification (if it's currently showing)
        SingletonServices.getNotificationManager(context).cancel(Notifications.MOONLIGHT_REMINDER_NOTIFICATION_ID);

        // Get moonlight reminder hours preference
        int moonlightReminderHours = AppPreferences.getMoonlightReminderHours(context);

        // Enabled?
        if (moonlightReminderHours > 0) {
            // Figure out when the reminder should be
            long remindMoonlightTimestamp = nextAlarm - (1000 * 60 * 60 * moonlightReminderHours);

            // Is it before the sunrise starts?
            if (remindMoonlightTimestamp < startSunriseTimestamp) {
                // Reminder should have already been displayed
                if (remindMoonlightTimestamp < System.currentTimeMillis()) {
                    // Display it in 100ms
                    remindMoonlightTimestamp = System.currentTimeMillis() + 100;
                }

                // If it's supposed to show up in the next 10 seconds
                if (remindMoonlightTimestamp < System.currentTimeMillis() + 10000) {
                    // Just show it, avoid scheduling
                    context.sendBroadcast(new Intent(context, ShowMoonlightReminder.class));
                }
                else {
                    // Schedule the moonlight reminder in the future
                    SingletonServices.getAlarmManager(context).setExact(AlarmManager.RTC_WAKEUP, remindMoonlightTimestamp, getMoonlightReminderPendingIntent(context));

                    // Log the scheduled reminder
                    Log.d(Logging.TAG, "Moonlight reminder in " + ((remindMoonlightTimestamp - System.currentTimeMillis()) / 1000 / 60 / 60) + " hours");
                }
            }
        }
    }

    private static PendingIntent getSunriseAlarmPendingIntent(Context context) {
        // Set the intent to start our sunrise alarm service
        Intent intent = new Intent(context, SunriseService.class);

        // Convert to pending intent
        return PendingIntent.getService(context, 0, intent, 0);
    }

    private static PendingIntent getMoonlightReminderPendingIntent(Context context) {
        // Set the intent to display a moonlight reminder
        Intent intent = new Intent(context, ShowMoonlightReminder.class);

        // Convert to pending intent
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static PendingIntent getHideReminderPendingIntent(Context context) {
        // Set the intent to hide the moonlight reminder
        Intent intent = new Intent(context, HideMoonlightReminder.class);

        // Convert to pending intent
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}
