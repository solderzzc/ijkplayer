/*
 * Copyright (C) 2015 Bilibili
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tv.danmaku.ijk.media.example.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.example.R;
import tv.danmaku.ijk.media.example.application.Settings;
import tv.danmaku.ijk.media.example.fragments.TracksFragment;
import tv.danmaku.ijk.media.example.widget.media.AndroidMediaController;
import tv.danmaku.ijk.media.example.widget.media.IjkVideoView;
import tv.danmaku.ijk.media.example.widget.media.MeasureHelper;

public class VideoActivity extends AppCompatActivity implements TracksFragment.ITrackHolder {
    private static final String TAG = "VideoActivity";

    private String mVideoURL;
    private String mVideoURL2;

    private AndroidMediaController mMediaController;
    private IjkVideoView mVideoView;
    private IjkVideoView mVideoView2;
    private TextView mToastTextView;
    private TableLayout mHudView;
    private DrawerLayout mDrawerLayout;
    private ViewGroup mRightDrawer;

    private Settings mSettings;
    private boolean mBackPressed;

    private long mLastFrameTimeStamp = 0L;

    private void quitAndStartLater() {
        Intent intent = new Intent(VideoActivity.this, VideoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //PendingIntent pendingIntent = PendingIntent.getActivity(VideoActivity.this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        Intent resultIntent = new Intent(this, SetupActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        mgr.set(AlarmManager.RTC, System.currentTimeMillis()+15000, pendingIntent);

        finish();
        System.exit(2);
    }
    public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        public ExceptionHandler() {
        }
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            ex.printStackTrace();
            Log.w("##RDBG", "uncaughtException, ex:" + ex.toString());
            quitAndStartLater();
        }
    }
    private static boolean mHasMotion;
    private static double mPixelDiff;

    public static Intent newIntent(Context context, String videoUrl) {
        Intent intent = new Intent(context, VideoActivity.class);
        intent.putExtra("videoUrl", videoUrl);
        return intent;
    }

    public static void intentTo(Context context, String videoUrl) {
        context.startActivity(newIntent(context, videoUrl));
    }
    public static double getPixelDiff(){
        return mPixelDiff;
    }

    public static void setPixelDiff(double pixedlDiff){
        mPixelDiff = pixedlDiff;
    }
    public static boolean getMotionStatus(){
        return mHasMotion;
    }

    public static void setMotionStatus(boolean motion){
        mHasMotion = motion;
    }
    public String getSavedCameraURL(){

        SharedPreferences sp = getSharedPreferences(CameraScanActivity.CAMERAIPKEY, Context.MODE_PRIVATE);
        mVideoURL = sp.getString("videoURL", "");

        return "";
        //String videoUrl = "rtsp://admin:abc12345@"+ci.getIp()+":554/cam/realmonitor?channel=1&subtype=0";
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        setContentView(R.layout.activity_player);

        mSettings = new Settings(this);

        // handle arguments
        mVideoURL = getIntent().getStringExtra("videoUrl");
        /*if (TextUtils.isEmpty(mVideoURL)) {
            Toast.makeText(this, "Camera IP is not set, please scan and set it first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }*/

        mVideoURL2 = "rtsp://admin:abc12345@192.168.31.222:554/cam/realmonitor?channel=1&subtype=0";

        // init UI
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        mMediaController = new AndroidMediaController(this, false);
        mMediaController.setSupportActionBar(actionBar);

        mToastTextView = (TextView) findViewById(R.id.toast_text_view);
        mHudView = (TableLayout) findViewById(R.id.hud_view);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mRightDrawer = (ViewGroup) findViewById(R.id.right_drawer);

        mDrawerLayout.setScrimColor(Color.TRANSPARENT);

        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        mVideoView = (IjkVideoView) findViewById(R.id.video_view);
        mVideoView.setMediaController(mMediaController);
        mVideoView.setHudView(mHudView);

        // prefer mVideoURL
        if (mVideoURL != null)
            mVideoView.setVideoRTSP(mVideoURL);
        else {
            Log.e(TAG, "Null Data Source\n");
            quitAndStartLater();
            return;
        }
        mVideoView.setVideoFrameUpdateListener(new IjkVideoView.VideoFrameUpdateListener() {
            @Override
            public void onVideoFrameUpdate(long tm) {
                mLastFrameTimeStamp = tm;
            }
        });
        mVideoView.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int what, int extra) {
                Log.w("##RDBG", "onErrorListener: " + what);
                quitAndStartLater();
                return false;
            }
        });
        mVideoView.start();

        mVideoView2 = (IjkVideoView) findViewById(R.id.video_view2);
        mVideoView2.setMediaController(mMediaController);
        mVideoView2.setHudView(mHudView);

        // prefer mVideoURL
        if (mVideoURL2 != null)
            mVideoView2.setVideoRTSP(mVideoURL2);
        else {
            Log.e(TAG, "Null Data Source\n");
            quitAndStartLater();
            return;
        }
        mVideoView2.setVideoFrameUpdateListener(new IjkVideoView.VideoFrameUpdateListener() {
            @Override
            public void onVideoFrameUpdate(long tm) {
                //mLastFrameTimeStamp = tm;
            }
        });
        mVideoView2.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int what, int extra) {
                Log.w("##RDBG", "onErrorListener: " + what);
                quitAndStartLater();
                return false;
            }
        });
        mVideoView2.start();

        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                long cur = System.currentTimeMillis();
                if (mLastFrameTimeStamp > 0 && cur - mLastFrameTimeStamp > 5000) {
                    Toast.makeText(VideoActivity.this, "frame timespan exceeds 5 seconds, exit!", Toast.LENGTH_LONG).show();

                    Log.w("##RDBG", "frame timespan exceeds 5 seconds");
                    quitAndStartLater();
                }
            }

        }, 20000, 10000);
    }

    @Override
    public void onBackPressed() {
        mBackPressed = true;

        super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBackPressed || !mVideoView.isBackgroundPlayEnabled()) {
            mVideoView.stopPlayback();
            mVideoView.release(true);
            mVideoView.stopBackgroundPlay();

            mVideoView2.stopPlayback();
            mVideoView2.release(true);
            mVideoView2.stopBackgroundPlay();
        } else {
            mVideoView.enterBackground();
            mVideoView2.enterBackground();
        }
        IjkMediaPlayer.native_profileEnd();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_toggle_ratio) {
            int aspectRatio = mVideoView.toggleAspectRatio();
            String aspectRatioText = MeasureHelper.getAspectRatioText(this, aspectRatio);
            mToastTextView.setText(aspectRatioText);
            mMediaController.showOnce(mToastTextView);
            return true;
        } else if (id == R.id.action_toggle_player) {
            int player = mVideoView.togglePlayer();
            String playerText = IjkVideoView.getPlayerText(this, player);
            mToastTextView.setText(playerText);
            mMediaController.showOnce(mToastTextView);
            return true;
        } else if (id == R.id.action_toggle_render) {
            int render = mVideoView.toggleRender();
            String renderText = IjkVideoView.getRenderText(this, render);
            mToastTextView.setText(renderText);
            mMediaController.showOnce(mToastTextView);
            return true;
        } else if (id == R.id.action_show_info) {
            mVideoView.showMediaInfo();
        } else if (id == R.id.action_show_tracks) {
            if (mDrawerLayout.isDrawerOpen(mRightDrawer)) {
                Fragment f = getSupportFragmentManager().findFragmentById(R.id.right_drawer);
                if (f != null) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.remove(f);
                    transaction.commit();
                }
                mDrawerLayout.closeDrawer(mRightDrawer);
            } else {
                Fragment f = TracksFragment.newInstance();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.right_drawer, f);
                transaction.commit();
                mDrawerLayout.openDrawer(mRightDrawer);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public ITrackInfo[] getTrackInfo() {
        if (mVideoView == null)
            return null;

        return mVideoView.getTrackInfo();
    }

    @Override
    public void selectTrack(int stream) {
        mVideoView.selectTrack(stream);
    }

    @Override
    public void deselectTrack(int stream) {
        mVideoView.deselectTrack(stream);
    }

    @Override
    public int getSelectedTrack(int trackType) {
        if (mVideoView == null)
            return -1;

        return mVideoView.getSelectedTrack(trackType);
    }
}
