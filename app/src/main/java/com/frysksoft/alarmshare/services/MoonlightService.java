package com.frysksoft.alarmshare.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.frysksoft.alarmshare.config.Logging;
import com.frysksoft.alarmshare.config.Notifications;
import com.frysksoft.alarmshare.utils.AppPreferences;
import com.frysksoft.alarmshare.utils.SingletonServices;
import com.frysksoft.alarmshare.utils.MqttManager;

public class MoonlightService extends Service {
    MqttManager mMQTT;
    AsyncMoonlightTask mAlarmTask;

    @Override
    public void onCreate() {
        super.onCreate();

        // Log startup
        Log.d(Logging.TAG, "MoonlightService started");

        // Get an instance of the Milight API client
        mMQTT = SingletonServices.getMQTT(this);

        // Clear moonlight reminder notification (if displayed)
        SingletonServices.getNotificationManager(this).cancel(Notifications.MOONLIGHT_REMINDER_NOTIFICATION_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The actual moonlight alarm logic (since we can't issue network calls on service's main thread)
        mAlarmTask = new AsyncMoonlightTask();

        // Run async task
        mAlarmTask.execute();

        // Don't restart this service
        return START_NOT_STICKY;
    }

    private void sendMoonlightCommands(Context context) throws Exception {
        // Get the amount of milliseconds to sleep after the sunrise alarm reaches 100%
        int moonlightDuration = (AppPreferences.getMoonlightDurationMinutes(context) * 60 * 1000);

        // Set brightness level to 0% and select the bulb (to avoid a 100% full blast if that was the bulb's previous state before it was turned off)
        mMQTT.setBrightness(0);

        // Get the desired moonlight brightness level
        int moonlightBrightness = AppPreferences.getMoonlightBrightnessLevel(context);

        // Set brightness level to desired brightness level
        mMQTT.setBrightness(moonlightBrightness);

        // Write to log
        Log.d(Logging.TAG, "Entering moonlight mode for " + moonlightDuration + "ms");

        // Wait X amount of milliseconds before turning the bulb off
        Thread.sleep(moonlightDuration);

        // Turn off the bulb after moonlight mode ends
        mMQTT.fadeOut();
    }

    @Override
    public void onDestroy() {
        // Write to log
        Log.d(Logging.TAG, "MoonlightService destroyed");

        // Currently running the AsyncTask?
        if (mAlarmTask != null) {
            // Cancel (and interrupt any threads that are currently sleeping)
            mAlarmTask.cancel(true);
        }

        // Now we're ready to be destroyed
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Don't allow binding to this service
        return null;
    }

    public class AsyncMoonlightTask extends AsyncTask<Boolean, String, Integer> {
        @Override
        protected Integer doInBackground(Boolean... param) {
            try {
                // Run the main moonlight code (may be interrupted by AsyncTask.cancel())
                sendMoonlightCommands(MoonlightService.this);
            }
            catch (Exception exc) {
                // Log errors to logcat
                Log.e(Logging.TAG, "MoonlightService error", exc);
            }

            // Gotta return something
            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            // All done with service, stop it now
            stopSelf();
        }
    }
}
