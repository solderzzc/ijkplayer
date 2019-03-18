package tv.danmaku.ijk.media.example.application;

import android.app.Application;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import tv.danmaku.ijk.media.example.mqtt.VideoMqttClient;

public class VideoApplication extends Application {
    private VideoMqttClient videoMqttClient;

    private AppCompatActivity activity;

    public void setActivity(AppCompatActivity activity) {
        this.activity = activity;
    }

    public AppCompatActivity getActivity() {
        return activity;
    }

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
