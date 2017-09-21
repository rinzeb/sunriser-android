package com.frysksoft.alarmshare.utils;

import android.content.Context;
import android.util.Log;

import com.frysksoft.alarmshare.config.Logging;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

/**
 * Created by rbruining on 20-9-2017.
 */

public class MqttManager {

    final static String PREFIX = "tcp://";

    String clientId = MqttClient.generateClientId();
    MqttAndroidClient client = null;

    public MqttManager(Context context, String mqttHost, int mqttPort) {
        this(context, mqttHost, mqttPort, null, null);
    }

    public MqttManager(Context context, String mqttHost, int mqttPort, String user, String password) {
        this.client = new MqttAndroidClient(context, PREFIX + mqttHost + ":" + Integer.toString(mqttPort), clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(user);
        options.setPassword(password.toCharArray());

        try {
            IMqttToken token = this.client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(Logging.TAG, "mqtt onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(Logging.TAG, "mqtt onFailure");
                    Log.d(Logging.TAG, exception.getMessage());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void sendCommand(String topic, String payload) {
        if (this.client == null || !this.client.isConnected()) {
            Log.w(Logging.TAG, "No mqtt connection, cannot send command");
            return;
        }

        byte[] encodedPayload;
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            this.client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    public void fadeOut() {
        // Send off command
        this.sendCommand("cmnd/bedroom/POWER", "OFF");
    }

    public void setBrightness(int i) {
        //Send brightness command
        if (i < 0) i = 0;
        if (i > 100) i = 100;
        this.sendCommand("cmnd/bedroom/Dimmer", Integer.toString(i));
    }

    public void startWake() {
        // Send start command
        this.sendCommand("cmnd/bedroom/Wakeup", "100");
    }

    public void setDuration(int i) {
        // Send start command
        this.sendCommand("cmnd/bedroom/WakeupDuration", Integer.toString(i));
    }

    public void fadeOn() {
        // Send start command
        this.sendCommand("cmnd/bedroom/POWER", "ON");
    }

    public boolean isConnected() {
        return this.client != null && this.client.isConnected();
    }
}
