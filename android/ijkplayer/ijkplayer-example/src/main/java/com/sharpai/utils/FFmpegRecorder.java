package com.sharpai.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import nl.bravobit.ffmpeg.FFcommandExecuteResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.FFtask;

public class FFmpegRecorder {
    //String RTSP_URL = "rtsp://<your_rtsp_url>";

    final static String TAG="FFMPEG";
    //final File targetFile = new File( getExternalStoragePublicDirectory( Environment.DIRECTORY_MOVIES )  + "/recording1.mp4" );

    FFmpeg mFFmpeg;
    TimerTask mTimerTask;
    FFtask mFFTask;
    public FFmpegRecorder(Context content,String url, File targetFile){
        String[] ffmpegCommand = new String[]{  "-i", url, "-acodec", "copy", "-vcodec", "copy", targetFile.toString() };
        Timer timer = new java.util.Timer();

        mFFmpeg = FFmpeg.getInstance(content);
        if(mFFmpeg.isSupported()){
            Log.d(TAG,"FFmpeg is supported");
        } else {
            Log.d(TAG,"FFmpeg is not supported");
        }
        mFFTask = mFFmpeg.execute( ffmpegCommand, new FFcommandExecuteResponseHandler() {

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
        mTimerTask = new TimerTask() { @Override public void run() {
            Log.d(TAG,"sendQuitSignal");
            mFFTask.sendQuitSignal();
        } };
        timer.schedule(mTimerTask, 30000 ); // Will stop recording after 30 seconds.
    }
    public void stopRecording(){
        mFFTask.sendQuitSignal();
        mTimerTask.cancel();
    }
}
