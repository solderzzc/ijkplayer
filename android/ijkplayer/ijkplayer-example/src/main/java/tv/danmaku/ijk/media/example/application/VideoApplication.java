package tv.danmaku.ijk.media.example.application;

import android.app.Application;
import android.util.Log;

import tv.danmaku.ijk.media.example.mqtt.VideoMqttClient;

public class VideoApplication extends Application {
    private VideoMqttClient videoMqttClient;

    public VideoApplication() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        videoMqttClient = new VideoMqttClient(this);
        videoMqttClient.startMqttClient();
        Log.d("##RDBG", "VideoApplication start mqtt");
    }
}
