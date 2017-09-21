package com.frysksoft.alarmshare.utils;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.frysksoft.alarmshare.config.Logging;

public class SingletonServices {
    private static MqttManager mMQTT;
    private static AlarmManager mAlarmManager;
    private static SharedPreferences mSharedPreferences;
    private static ConnectivityManager mConnectivityManager;
    private static NotificationManager mNotificationManager;

    public static MqttManager getMQTT(Context context) {
        // First time?
        if (mMQTT == null) {
            try {
                // Instantiate a new instance of the API client
                mMQTT = new MqttManager(context, AppPreferences.getMqttHost(context), AppPreferences.getMqttPort(context));
            }
            catch(Exception err) {
                // Log to console and return null
                Log.e(Logging.TAG, "MQTT instantiation failed", err);
            }
        }

        // Return cached instance
        return mMQTT;
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        // First time?
        if (mSharedPreferences == null) {
            // Acquire system service
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }

        // Return cached instance
        return mSharedPreferences;
    }

    public static AlarmManager getAlarmManager(Context context) {
        // First time?
        if (mAlarmManager == null) {
            // Acquire system service
            mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }

        // Return cached instance
        return mAlarmManager;
    }

    public static ConnectivityManager getConnectivityManager(Context context) {
        // First time?
        if (mConnectivityManager == null) {
            // Acquire system service
            mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        // Return cached instance
        return mConnectivityManager;
    }

    public static NotificationManager getNotificationManager(Context context) {
        // First time?
        if (mNotificationManager == null) {
            // Acquire system service
            mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        // Return cached instance
        return mNotificationManager;
    }
}
