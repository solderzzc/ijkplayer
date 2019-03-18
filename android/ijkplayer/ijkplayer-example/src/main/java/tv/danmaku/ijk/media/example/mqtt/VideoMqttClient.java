package tv.danmaku.ijk.media.example.mqtt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import tv.danmaku.ijk.media.example.activities.SetupActivity;
import tv.danmaku.ijk.media.example.activities.VideoActivity;
import tv.danmaku.ijk.media.example.application.VideoApplication;

public class VideoMqttClient implements MqttCallback {
    private Context context;

    private MqttClient mqttClient;
    public static final String MQTT_BROKER_URL = "tcp://mq.tiegushi.com:8080";
    private boolean mConnectedToBroker = false;
    public static final String CAMERAIPKEY = "tv.danmaku.ijk.media.example.activities.CameraScanActivity.VideoSettings";

    private String deviceMac = null;

    /*
     * Load file content to String
     */
    public static String loadFileAsString(String filePath) throws java.io.IOException{
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }

    /*
     * Get the STB MacAddress
     */
    public static String getMacAddress(){
        try {
            String mac = null;
            mac = loadFileAsString("/sys/class/net/eth0/address")
                    .toUpperCase().substring(0, 17);
            String[] strArr = mac.split(":");
            mac = TextUtils.join("", strArr);
            return mac;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void startMqttClient() {
        deviceMac = getMacAddress();
        startIntervalForMQTTReconnect();
    }

    private void startIntervalForMQTTReconnect() {
        Log.d("##RDBG", "startIntervalForMQTTReconnect");
        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mConnectedToBroker == false) {
                    reConnectToMQTT();
                }
            }
        }, 1000, 6000);
    }

    private void reConnectToMQTT() {
        String topic = "/camerasettings";
        Log.d("##RDBG", "reConnectToMQTT");
        mConnectedToBroker = false;
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            mqttClient = new MqttClient(MQTT_BROKER_URL, MqttClient.generateClientId(), persistence);
            mqttClient.setCallback(this);
            mqttClient.connect();
            mqttClient.subscribe(topic);
            mqttClient.subscribe("ijkplayertest");
            final MqttMessage message = new MqttMessage("testing".getBytes());
            mqttClient.publish("ijkplayertest", message);
            Log.d("##RDBG", "connecting mqtt");
        }
        catch (MqttException e) {
            e.printStackTrace();
            mConnectedToBroker = false;
            Log.d("##RDBG", "connecting mqtt exception");
        }
        catch (Exception ex) {
            ex.printStackTrace();
            Log.d("##RDBG", "connecting mqtt exception");
        }
    }

    public  VideoMqttClient(Context ctx) {
        context = ctx;
    }

    @Override
    public void connectionLost(Throwable throwable) {
        mConnectedToBroker = false;
        Log.d("##RDBG", "connectionLost");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Log.d("##RDBG", "messageArrived, topic: " + topic + ", message: " + message.toString());
        if (topic.equals("ijkplayertest")) {
            mConnectedToBroker = true;

        }
        else if (topic.equals("/camerasettings")) {
            JSONObject msgObj = new JSONObject(message.toString());
            String uuid = msgObj.getString("uuid");
            //deviceMac = "321ecde3d934";
            if (uuid != null && uuid.equalsIgnoreCase(deviceMac)) {
                String ip = msgObj.getString("camip");
                String username = msgObj.getString("camusername");
                String password = msgObj.getString("campassword");
                Log.d("##RDBG", "camerasettings, ip: " + ip + ", username: " + username + ",password: " + password);
                saveCameraSettings(ip, username, password, uuid);

                quitAndStartLater();
            }
        }
    }

    private void quitAndStartLater() {
        AppCompatActivity activity = ((VideoApplication)context).getActivity();
        if (activity == null)
            return;

        Intent intent = new Intent(activity, SetupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //PendingIntent pendingIntent = PendingIntent.getActivity(VideoActivity.this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        Intent resultIntent = new Intent(activity, SetupActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(activity);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager mgr = (AlarmManager)activity.getSystemService(Context.ALARM_SERVICE);

        mgr.set(AlarmManager.RTC, System.currentTimeMillis()+15000, pendingIntent);

        activity.finish();
        System.exit(2);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Log.d("##RDBG", "deliveryComplete");
    }

    private void saveCameraSettings(String ip, String username, String password, String uuid){

        SharedPreferences sp = context.getSharedPreferences(CAMERAIPKEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("cameraUsername", username);
        editor.putString("cameraPassword", password);
        String videoUrl = "rtsp://admin:abc12345@"+ip+":554/cam/realmonitor?channel=1&subtype=0";
        editor.putString("videoURL", videoUrl);
        editor.putString("videoIP", ip);
        editor.putString("cameraSN", deviceMac);
        editor.commit();
    }
}
