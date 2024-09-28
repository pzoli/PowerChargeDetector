package hu.infokristaly.powerchargedetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.telephony.SmsManager;
import android.util.JsonWriter;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.nio.charset.StandardCharsets;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

public class PlugInControlReceiver extends BroadcastReceiver {

    static class DTOStruct {
        String action;
        int status;
        int chargePlug;
        boolean isCharging;
        boolean usbCharge;
        boolean acCharge;
        float batteryPct;
    }
    MqttAndroidClient client;

    private Boolean ACPowerStatus = null;

    public  PlugInControlReceiver(MqttAndroidClient client) {
        this.client = client;
    }

    public void onReceive(Context context , Intent intent) {
        DTOStruct statusObj = new DTOStruct();
        statusObj.action = intent.getAction();
        if (statusObj.action != null) {
            if (statusObj.action.equals(Intent.ACTION_BATTERY_CHANGED)) {


                statusObj.status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                statusObj.isCharging = statusObj.status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        statusObj.status == BatteryManager.BATTERY_STATUS_FULL;

                statusObj.chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                statusObj.usbCharge = statusObj.chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
                statusObj.acCharge = statusObj.chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                statusObj.batteryPct = level * 100 / (float) scale;

                if (ACPowerStatus == null) {
                    ACPowerStatus = statusObj.isCharging;
                } else {
                    JSONObject jsonObj = new JSONObject();
                    try {
                        jsonObj.put("status",statusObj.status);
                        jsonObj.put("isCharging", statusObj.isCharging);
                        if (statusObj.isCharging) {
                            jsonObj.put("charger", statusObj.acCharge ? "AC" : statusObj.usbCharge ? "USB":"");
                        }
                        jsonObj.put("batteryPct",statusObj.batteryPct);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    String message = jsonObj.toString();
                    if (ACPowerStatus != statusObj.isCharging) {
                        boolean sendSMS = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("send_sms",false);
                        if (sendSMS) {
                            String phoneNumber = PreferenceManager.getDefaultSharedPreferences(context).getString("alert_phone_number", "");
                            if (!phoneNumber.isEmpty()) {
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(phoneNumber, null, "AC power charging: " + (statusObj.isCharging ? "true" : "false"), null, null);
                            }
                        }
                        ACPowerStatus = statusObj.isCharging;
                        if (client != null && client.isConnected()) {
                            String mqttTopic = PreferenceManager.getDefaultSharedPreferences(context).getString("mqtt_topic","");
                            client.publish(mqttTopic, new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
                        }
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(context, "Action: " + statusObj.action, Toast.LENGTH_LONG).show();
            }
        }
    }
}
