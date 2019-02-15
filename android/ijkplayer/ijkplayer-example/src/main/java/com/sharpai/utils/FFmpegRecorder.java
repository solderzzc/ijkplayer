package com.sharpai.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import nl.bravobit.ffmpeg.FFcommandExecuteResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.FFtask;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class FFmpegRecorder {
    //String RTSP_URL = "rtsp://<your_rtsp_url>";

    final static String TAG="FFMPEG";
    //final File targetFile = new File( getExternalStoragePublicDirectory( Environment.DIRECTORY_MOVIES )  + "/recording1.mp4" );


    public FFmpegRecorder(Context content,String url, File targetFile){
        String[] ffmpegCommand = new String[]{  "-i", url, "-acodec", "copy", "-vcodec", "copy", targetFile.toString() };
        Timer timer = new java.util.Timer();

        FFmpeg ffmpeg = FFmpeg.getInstance(content);
        //if(ffmpeg.isSupported()){
        Log.d(TAG,"is supported"+ffmpeg.isSupported());
        //}
        FFtask ffTask = ffmpeg.execute( ffmpegCommand, new FFcommandExecuteResponseHandler() {

            @Override
            public void onStart() {
                Log.d(TAG,"onStart");
            }

            @Override
            public void onProgress(String message) {
                Log.d(TAG,"onProgress "+message);
            }

            @Override
            public void onFailure(String message) {
                Log.d(TAG,"onFailure "+message);
            }

            @Override
            public void onSuccess(String message) {
                Log.d(TAG,"onSuccess "+message);
            }

            @Override
            public void onFinish() {
                Log.d(TAG,"onFinish");
            }

        } );
        TimerTask timerTask = new TimerTask() { @Override public void run() {
            Log.d(TAG,"sendQuitSignal");
            ffTask.sendQuitSignal();
        } };
        timer.schedule( timerTask, 30000 ); // Will stop recording after 30 seconds.
    }

}
