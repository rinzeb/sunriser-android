package com.frysksoft.alarmshare.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.frysksoft.alarmshare.utils.MqttManager;
import com.frysksoft.alarmshare.config.Logging;
import com.frysksoft.alarmshare.utils.AppPreferences;
import com.frysksoft.alarmshare.utils.Networking;
import com.frysksoft.alarmshare.utils.SingletonServices;
import com.frysksoft.alarmshare.utils.intents.IntentExtras;

import java.util.Timer;
import java.util.TimerTask;

public class SunriseService extends Service {
    Timer mSunriseTimer;

    boolean mIsTesting;
    int mCurrentBrightness;

    MqttManager mMQTT;

    @Override
    public void onCreate() {
        super.onCreate();

        // Log startup
        Log.d(Logging.TAG, "SunriseService started");

        // Get an instance of the Milight API client
        mMQTT = SingletonServices.getMQTT(this);

        // Create timers to handle future tasks
        mSunriseTimer = new Timer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // App disabled or no Wi-Fi connection?
        if (!AppPreferences.isAppEnabled(this) || !Networking.isWiFiConnected(this)) {
            stopSelf();
        }

        // Reset current brightness level
        mCurrentBrightness = 0;

        // Determine whether we are testing the alarm
        mIsTesting = intent.getBooleanExtra(IntentExtras.SUNRISE_ALARM_TEST, false);

        // Start the sunrise (schedule future brightness updates and daylight kill)
        mSunriseTimer.schedule(new StartSunriseTask(), 0);

        // Cancel any scheduled daylight kill
        SingletonServices.getAlarmManager(SunriseService.this).cancel(getDaylightCompletedPendingIntent(SunriseService.this));

        // Don't restart this service
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Write to log
        Log.d(Logging.TAG, "SunriseService destroyed");

        // Cancel any pending timers
        mSunriseTimer.cancel();

        // Now we're ready to be destroyed
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Don't allow binding to this service
        return null;
    }

    private class StartSunriseTask extends TimerTask {
        @Override
        public void run() {
            try {
                // Write to log
                Log.d(Logging.TAG, "Starting sunrise");

                int sunriseDurationSeconds = (AppPreferences.getSunriseDurationMinutes(SunriseService.this) * 60);

                // Override interval if testing (to emit a fast sunrise)
                if (mIsTesting) {
                    sunriseDurationSeconds = 10;
                }

                // Make sure update interval is at least 50ms for timer interval to work correctly
                sunriseDurationSeconds = (sunriseDurationSeconds < 1) ? 1 : sunriseDurationSeconds;
                sunriseDurationSeconds = (sunriseDurationSeconds > 3600) ? 3600 : sunriseDurationSeconds;

                // Set the sunrise duration
                mMQTT.setDuration(sunriseDurationSeconds);

                // Turn on the bulb
                mMQTT.startWake();

                // Write update interval to log
                Log.d(Logging.TAG, "Starting sunrise, duration " + sunriseDurationSeconds + "m");

                // Wait till finish
                Thread.sleep((sunriseDurationSeconds * 1000) + 10000);

                // Did the user enable "Daylight Forever"?
                if (AppPreferences.isDaylightForeverEnabled(SunriseService.this)) {
                    // Write to log
                    Log.d(Logging.TAG, "Entering daylight mode forever");

                    // All done with service, stop it now
                    stopSelf();
                    return;
                }

                // Get the amount of milliseconds to sleep after the sunrise alarm reaches 100%
                int daylightDuration = (AppPreferences.getDaylightDurationMinutes(SunriseService.this) * 60 * 1000);

                // Testing?
                if (mIsTesting) {
                    // Kill daylight after a moment
                    daylightDuration = 1500;
                }

                // Log daylight kill scheduling
                Log.d(Logging.TAG, "Scheduling daylight kill " + daylightDuration + "ms from now");

                // Convert to timestamp
                long daylightKillTimestamp = System.currentTimeMillis() + daylightDuration;

                // Schedule it to be killed in the future
                SingletonServices.getAlarmManager(SunriseService.this).setExact(AlarmManager.RTC_WAKEUP, daylightKillTimestamp, getDaylightCompletedPendingIntent(SunriseService.this));

                // All done with service, stop it now
                stopSelf();
            }
            catch (Exception exc) {
                // Log errors to logcat
                Log.e(Logging.TAG, "StartSunriseTask error", exc);
            }
        }
    }

    private static PendingIntent getDaylightCompletedPendingIntent(Context context) {
        // Set the intent to hide the moonlight reminder
        Intent intent = new Intent(context, KillDaylightService.class);

        // Convert to pending intent
        return PendingIntent.getService(context, 0, intent, 0);
    }
}
