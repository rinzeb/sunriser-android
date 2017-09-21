package com.frysksoft.alarmshare.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.frysksoft.alarmshare.R;
import com.frysksoft.alarmshare.alarms.SystemClock;
import com.frysksoft.alarmshare.config.Logging;
import com.frysksoft.alarmshare.scheduler.SunriseScheduler;
import com.frysksoft.alarmshare.services.MoonlightService;
import com.frysksoft.alarmshare.services.SunriseService;
import com.frysksoft.alarmshare.utils.Networking;
import com.frysksoft.alarmshare.utils.SingletonServices;
import com.frysksoft.alarmshare.utils.intents.IntentExtras;

public class Main extends AppCompatActivity {
    ImageView mIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up activity UI
        initializeUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Force internet connection to use the app
        requireConnectivity();

        // Force mqtt connection
        requireMqtt();

        // Check for a scheduled system alarm
        requireScheduledSystemAlarm();

        // Schedule sunrise alarm based on system alarm clock (cancel any previously-scheduled sunrise alarms as well)
        SunriseScheduler.rescheduleSunriseAlarm(this, true);
    }

    void initializeUI() {
        // Inflate main activity layout
        setContentView(R.layout.activity_main);

        // Support action bar for older devices
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        // Set up a toolbar for old devices
        setSupportActionBar(toolbar);

        // Fetch view by ID
        mIcon = (ImageView) findViewById(R.id.icon);

        // Handle clicks on image
        mIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reschedule sunrise alarm (and display a toast)
                SunriseScheduler.rescheduleSunriseAlarm(Main.this, true);
            }
        });
    }

    private void testSunriseAlarm() {
        // Kill the sunrise alarm service (in case it's running)
        stopService(new Intent(this, SunriseService.class));

        // Kill the moonlight service (in case it's running)
        stopService(new Intent(this, MoonlightService.class));

        // Prepare sunrise alarm service intent
        Intent testSunrise = new Intent(Main.this, SunriseService.class);

        // Set to "test" mode
        testSunrise.putExtra(IntentExtras.SUNRISE_ALARM_TEST, true);

        // Start the service for testing
        startService(testSunrise);
    }

    private void requireConnectivity() {
        // No connection?
        if (!Networking.isConnected(this)) {
            // Show error dialog
            new AlertDialog.Builder(this)
                    .setTitle(R.string.connect_internet)
                    .setMessage(R.string.connect_internet_desc)
                    .setPositiveButton(R.string.ok, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            // Still no connection?
                            if (!Networking.isConnected(Main.this)) {
                                // Goodbye
                                finish();
                            }
                        }
                    })
                    .create().show();
        }
    }

    private void requireMqtt() {
        if (!SingletonServices.getMQTT(Main.this).isConnected()) {
            // Show error dialog
            new AlertDialog.Builder(this)
                    .setTitle(R.string.connect_mqtt)
                    .setMessage(R.string.connect_mqtt_desc)
                    .setPositiveButton(R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            // Still no connection?
                            startActivity(new Intent(Main.this, Settings.class));
                        }
                    })
                    .create().show();
        }
    }

    private void requireScheduledSystemAlarm() {
        // No scheduled alarm?
        if (SystemClock.getNextAlarmTriggerTimestamp(this) == 0) {
            // Show error dialog
            new AlertDialog.Builder(this)
                    .setTitle(R.string.no_alarms)
                    .setMessage(R.string.no_alarms_desc)
                    .setPositiveButton(R.string.ok, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            // Goodbye
                            finish();
                        }
                    })
                    .create().show();
        }
    }

    private void viewSettings() {
        // Start the settings activity
        startActivity(new Intent(Main.this, Settings.class));
    }

    private void enableMoonlightMode() {
        // Kill the sunrise alarm service (in case it's running)
        stopService(new Intent(this, SunriseService.class));

        // Kill the previous moonlight service (in case it's running)
        stopService(new Intent(this, MoonlightService.class));

        // (Re)start the moonlight service
        startService(new Intent(this, MoonlightService.class));
    }

    private void killLight() {
        // Kill the sunrise alarm service (in case it's running)
        stopService(new Intent(this, SunriseService.class));

        // Kill the moonlight service (in case it's running)
        stopService(new Intent(this, MoonlightService.class));

        // Actually turn off the light (by zone)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Turn off the light for the selected zone
                    SingletonServices.getMQTT(Main.this).fadeOut();
                }
                catch (Exception exc) {
                    // Log errors to logcat
                    Log.e(Logging.TAG, "Kill error", exc);
                }
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu - this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks by ID
        switch (item.getItemId()) {
            // Test
            case R.id.action_test:
                testSunriseAlarm();
                return true;
            // Kill
            case R.id.action_kill_light:
                killLight();
                return true;
            // Moonlight
            case R.id.action_moonlight:
                enableMoonlightMode();
                return true;
            // Settings
            case R.id.action_settings:
                viewSettings();
                return true;
        }

        // Don't consume the event
        return super.onOptionsItemSelected(item);
    }
}
