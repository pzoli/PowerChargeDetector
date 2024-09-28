package hu.infokristaly.powerchargedetector;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import hu.infokristly.Test;
import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;
import info.mqtt.android.service.MqttTraceHandler;

public class MyService extends Service {
    public static boolean isRunning = false;
    PlugInControlReceiver receiver;

    MqttAndroidClient client;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        isRunning = true;
        Toast.makeText(this, "Service started", Toast.LENGTH_LONG).show();
        Test.Companion.log("It's work!");

        createMQTTClient();
    }

    private void createMQTTClient() {
        Context context = getApplicationContext();
        String mqttServer = PreferenceManager.getDefaultSharedPreferences(context).getString("mqtt_server","");
        try {
            client = new MqttAndroidClient(context, mqttServer, "Pepe", Ack.AUTO_ACK);
            receiver = new PlugInControlReceiver(client);
            context.registerReceiver(receiver, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
            context.registerReceiver(receiver, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
            ;
            context.registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            client.setTraceEnabled(true);
            client.setTraceCallback(new MqttTraceHandler() {
                @Override
                public void traceError(@Nullable String s) {
                    System.err.println("traceError message:" + s);
                }

                @Override
                public void traceDebug(@Nullable String s) {
                    System.out.println("trace debug message:" + s);
                }

                @Override
                public void traceException(@Nullable String s, @Nullable Exception e) {
                    System.err.println("traceException:" + s + ", message:" + e.getMessage());
                }

            });
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                }

                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println(topic + ": " + new String(message.getPayload()));
                    Toast.makeText(context, new String(message.getPayload()), Toast.LENGTH_LONG).show();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Delivery complete");
                }
            });
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(false);
            options.setUserName("");
            options.setPassword("".toCharArray());
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_DEFAULT);
            try {
                IMqttToken token = client.connect(options, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        System.out.println("Connection success");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        System.out.println("Connection failed");
                    }
                });

            } catch (Exception e) {
                System.err.println((e.getMessage()));
            }
        } catch (Exception e) {
            System.err.println((e.getMessage()));
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (client != null && client.isConnected()) {
            try {
                Context context = getApplicationContext();
                String mqttTopic = PreferenceManager.getDefaultSharedPreferences(context).getString("mqtt_topic", "");
                client.unsubscribe(mqttTopic);
                client.disconnect();
            } catch (Exception e) {
                System.err.println((e.getMessage()));
            }
        }
        Context context = getApplicationContext();
        context.unregisterReceiver(receiver);
    }
}
