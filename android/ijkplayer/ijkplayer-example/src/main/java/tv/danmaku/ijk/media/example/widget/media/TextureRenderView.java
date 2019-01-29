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

package tv.danmaku.ijk.media.example.widget.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v8.renderscript.RenderScript;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.mtcnn_as.FaceDetector;
import com.sharpai.detector.Classifier;
import com.sharpai.detector.Detector;
import com.sharpai.pim.MotionDetectionRS;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import elanic.in.rsenhancer.processing.RSImageProcessor;
import tv.danmaku.ijk.media.example.activities.VideoActivity;
import tv.danmaku.ijk.media.example.utils.screenshot;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.ISurfaceTextureHolder;
import tv.danmaku.ijk.media.player.ISurfaceTextureHost;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class TextureRenderView extends GLTextureView implements IRenderView {
    private static final String TAG = "TextureRenderView   ";
    private MeasureHelper mMeasureHelper;
    private Context mContext;
    private Handler mBackgroundHandler;

    private static final int PROCESS_SAVED_IMAGE_MSG = 1002;
    private static final int PROCESS_SAVED_IMAGE_MSG_NOTNOW = 2001;

    private int DETECTION_IMAGE_WIDTH = 854;
    private int DETECTION_IMAGE_HEIGHT = 480;
    private int PREVIEW_IMAGE_WIDTH = 1920;
    private int PREVIEW_IMAGE_HEIGHT = 1080;

    private static final int PROCESS_FRAMES_AFTER_MOTION_DETECTED = 3;

    private RenderScript mRS = null;
    private MotionDetectionRS mMotionDetection;
    private RSImageProcessor mRSProcessor;

    private FrameUpdateListener mFrameUpdateListener = null;

    private Detector mDetector = null;
    private FaceDetector mFaceDetector = null;

    private long mLastBigChangeTime = 0L;
    private int mSavingCounter = 0;
    private TextureRenderView mTextureRender;
    public interface FrameUpdateListener {
        public void onFrameUpdate(long currentTime);
    }

    public void setFrameUpdateListener(FrameUpdateListener l) {
        mFrameUpdateListener = l;
    }

    /**
     * Initializes the UI and initiates the creation of a motion detector.
     */
    public void initDetectionContext() {
        String devModel = Build.MODEL;
        /*if (devModel != null && devModel.equals("JDN-W09") && PREVIEW_IMAGE_HEIGHT>960) {
            PREVIEW_IMAGE_WIDTH = 1280;
            PREVIEW_IMAGE_HEIGHT = 960;
        }*/

        DETECTION_IMAGE_HEIGHT = DETECTION_IMAGE_WIDTH * PREVIEW_IMAGE_HEIGHT  / PREVIEW_IMAGE_WIDTH;
        Log.i(TAG,"DETECTION_IMAGE_HEIGHT " + DETECTION_IMAGE_HEIGHT);

        mRS = RenderScript.create(mContext);
        mMotionDetection = new MotionDetectionRS(mContext.getSharedPreferences(
                MotionDetectionRS.PREFS_NAME, Context.MODE_PRIVATE),mRS,
                PREVIEW_IMAGE_WIDTH,PREVIEW_IMAGE_HEIGHT,DETECTION_IMAGE_WIDTH,DETECTION_IMAGE_HEIGHT);
        mRSProcessor = new RSImageProcessor(mRS);
        mRSProcessor.initialize(DETECTION_IMAGE_WIDTH, DETECTION_IMAGE_HEIGHT);

        mDetector = new Detector(mContext);
        mFaceDetector = new FaceDetector(mContext);
    }
    class MyCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {

            File file = null;
            URL url;
            HttpURLConnection urlConnection = null;
            switch (msg.what) {
                case PROCESS_SAVED_IMAGE_MSG:
                    Log.d(TAG, "Processing file: " + msg.obj);
                    file = new File(msg.obj.toString());
                    try {
                        url = new URL("http://127.0.0.1:" + 3000 + "/api/post?url=" + msg.obj);

                        urlConnection = (HttpURLConnection) url
                                .openConnection();

                        int responseCode = urlConnection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            Log.d(TAG, "connect success ");
                        } else {
                            file.delete();
                        }
                    } catch (Exception e) {
                        file.delete();
                        urlConnection = null;
                        //e.printStackTrace();
                        Log.v(TAG, "Detector is not running");
                    } finally {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                            return true;
                        }
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    }
    public TextureRenderView(Context context) {
        super(context);
        mContext = context;
        mTextureRender = this;
        HandlerThread handlerThread = new HandlerThread("BackgroundThread");
        handlerThread.start();
        MyCallback callback = new MyCallback();
        mBackgroundHandler = new Handler(handlerThread.getLooper(), callback);

        initView(context);
        initDetectionContext();
    }

    public TextureRenderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTextureRender = this;
        initView(context);
        initDetectionContext();
    }

    public TextureRenderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTextureRender = this;
        initView(context);
        initDetectionContext();
    }
    /*
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public TextureRenderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            initView(context);
            initDetectionContext();
        }
    */
    private void initView(Context context) {
        mMeasureHelper = new MeasureHelper(this);
        mSurfaceCallback = new SurfaceCallback(this);
        setSurfaceTextureListener(mSurfaceCallback);
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public boolean shouldWaitForResize() {
        return false;
    }

    @Override
    protected void onDetachedFromWindow() {
        mSurfaceCallback.willDetachFromWindow();
        super.onDetachedFromWindow();
        mSurfaceCallback.didDetachFromWindow();
    }

    //--------------------
    // Layout & Measure
    //--------------------
    @Override
    public void setVideoSize(int videoWidth, int videoHeight) {
        if (videoWidth > 0 && videoHeight > 0) {
            mMeasureHelper.setVideoSize(videoWidth, videoHeight);
            requestLayout();
        }
    }

    @Override
    public void setVideoSampleAspectRatio(int videoSarNum, int videoSarDen) {
        if (videoSarNum > 0 && videoSarDen > 0) {
            mMeasureHelper.setVideoSampleAspectRatio(videoSarNum, videoSarDen);
            requestLayout();
        }
    }

    @Override
    public void setVideoRotation(int degree) {
        mMeasureHelper.setVideoRotation(degree);
        setRotation(degree);
    }

    @Override
    public void setAspectRatio(int aspectRatio) {
        mMeasureHelper.setAspectRatio(aspectRatio);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mMeasureHelper.doMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mMeasureHelper.getMeasuredWidth(), mMeasureHelper.getMeasuredHeight());
    }

    //--------------------
    // TextureViewHolder
    //--------------------

    public IRenderView.ISurfaceHolder getSurfaceHolder() {
        return new InternalSurfaceHolder(this, mSurfaceCallback.mSurfaceTexture, mSurfaceCallback);
    }

    private static final class InternalSurfaceHolder implements IRenderView.ISurfaceHolder {
        private TextureRenderView mTextureView;
        private SurfaceTexture mSurfaceTexture;
        private ISurfaceTextureHost mSurfaceTextureHost;

        public InternalSurfaceHolder(@NonNull TextureRenderView textureView,
                                     @Nullable SurfaceTexture surfaceTexture,
                                     @NonNull ISurfaceTextureHost surfaceTextureHost) {
            mTextureView = textureView;
            mSurfaceTexture = surfaceTexture;
            mSurfaceTextureHost = surfaceTextureHost;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public void bindToMediaPlayer(IMediaPlayer mp) {
            Log.d(TAG,"GL bindToMediaPlayer");
            if (mp == null)
                return;

            mp.setSurface(new Surface(mTextureView.mRender.getVideoTexture()));
            /*if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) &&
                    (mp instanceof ISurfaceTextureHolder)) {
                ISurfaceTextureHolder textureHolder = (ISurfaceTextureHolder) mp;
                mTextureView.mSurfaceCallback.setOwnSurfaceTexture(false);

                SurfaceTexture surfaceTexture = textureHolder.getSurfaceTexture();
                if (surfaceTexture != null) {
                    mTextureView.setSurfaceTexture(surfaceTexture);
                } else {
                    textureHolder.setSurfaceTexture(mSurfaceTexture);
                    textureHolder.setSurfaceTextureHost(mTextureView.mSurfaceCallback);
                }
            } else {
                mp.setSurface(openSurface());
            }*/
        }

        @NonNull
        @Override
        public IRenderView getRenderView() {
            return mTextureView;
        }

        @Nullable
        @Override
        public SurfaceHolder getSurfaceHolder() {
            return null;
        }

        @Nullable
        @Override
        public SurfaceTexture getSurfaceTexture() {
            return mSurfaceTexture;
        }

        @Nullable
        @Override
        public Surface openSurface() {
            if (mSurfaceTexture == null)
                return null;
            return new Surface(mSurfaceTexture);
        }
    }
    FastVideoTextureRenderer mRender;
    //-------------------------
    // SurfaceHolder.Callback
    //-------------------------

    @Override
    public void addRenderCallback(IRenderCallback callback) {
        mSurfaceCallback.addRenderCallback(callback);
    }

    @Override
    public void removeRenderCallback(IRenderCallback callback) {
        mSurfaceCallback.removeRenderCallback(callback);
    }

    private SurfaceCallback mSurfaceCallback;

    class SurfaceCallback implements TextureView.SurfaceTextureListener, ISurfaceTextureHost {
        private long mStartTime = 0;
        private SurfaceTexture mSurfaceTexture;
        private boolean mIsFormatChanged;
        private int mWidth;
        private int mHeight;

        private boolean mOwnSurfaceTexture = true;
        private boolean mWillDetachFromWindow = false;
        private boolean mDidDetachFromWindow = false;

        private WeakReference<TextureRenderView> mWeakRenderView;
        private Map<IRenderCallback, Object> mRenderCallbackMap = new ConcurrentHashMap<IRenderCallback, Object>();


        public SurfaceCallback(@NonNull TextureRenderView renderView) {
            mWeakRenderView = new WeakReference<TextureRenderView>(renderView);
        }

        public void setOwnSurfaceTexture(boolean ownSurfaceTexture) {
            mOwnSurfaceTexture = ownSurfaceTexture;
        }

        public void addRenderCallback(@NonNull IRenderCallback callback) {
            mRenderCallbackMap.put(callback, callback);

            ISurfaceHolder surfaceHolder = null;
            if (mSurfaceTexture != null) {
                if (surfaceHolder == null)
                    surfaceHolder = new InternalSurfaceHolder(mWeakRenderView.get(), mSurfaceTexture, this);
                callback.onSurfaceCreated(surfaceHolder, mWidth, mHeight);
            }

            if (mIsFormatChanged) {
                if (surfaceHolder == null)
                    surfaceHolder = new InternalSurfaceHolder(mWeakRenderView.get(), mSurfaceTexture, this);
                callback.onSurfaceChanged(surfaceHolder, 0, mWidth, mHeight);
            }
        }

        public void removeRenderCallback(@NonNull IRenderCallback callback) {
            mRenderCallbackMap.remove(callback);
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurfaceTexture = surface;
            mIsFormatChanged = false;
            mWidth = 0;
            mHeight = 0;
            mRender = new FastVideoTextureRenderer(mContext,surface,width,height,mTextureRender);

            Log.d(TAG,"GL onSurfaceTextureAvailable");

            ISurfaceHolder surfaceHolder = new InternalSurfaceHolder(mWeakRenderView.get(), surface, this);
            for (IRenderCallback renderCallback : mRenderCallbackMap.keySet()) {
                renderCallback.onSurfaceCreated(surfaceHolder, 0, 0);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            mSurfaceTexture = surface;
            mIsFormatChanged = true;
            mWidth = width;
            mHeight = height;

            ISurfaceHolder surfaceHolder = new InternalSurfaceHolder(mWeakRenderView.get(), surface, this);
            for (IRenderCallback renderCallback : mRenderCallbackMap.keySet()) {
                renderCallback.onSurfaceChanged(surfaceHolder, 0, width, height);
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mSurfaceTexture = surface;
            mIsFormatChanged = false;
            mWidth = 0;
            mHeight = 0;

            ISurfaceHolder surfaceHolder = new InternalSurfaceHolder(mWeakRenderView.get(), surface, this);
            for (IRenderCallback renderCallback : mRenderCallbackMap.keySet()) {
                renderCallback.onSurfaceDestroyed(surfaceHolder);
            }

            Log.d(TAG, "onSurfaceTextureDestroyed: destroy: " + mOwnSurfaceTexture);
            return mOwnSurfaceTexture;
        }
        private void _processFrame(SurfaceTexture surface){
            Bitmap bmp= mWeakRenderView.get().getBitmap();
            String filename = "";
            File file = null;
            int personNum = 0;
            boolean bigChanged = false;

            List<Classifier.Recognition> result =  mDetector.processImage(bmp);

            personNum = result.size();
            VideoActivity.setNumberOfPerson(personNum);

            // if no bigchange, but timespan between two uploaded frames is larger than 30s, treat it as big change
            if (personNum<=0) {
                long tm = System.currentTimeMillis();
                if (tm - mLastBigChangeTime > 30*1000) {
                    bigChanged = true;
                }
            } else {
                bigChanged = true;
            }
            if(!bigChanged){
                Log.d(TAG,"No person detected, skip this frame");

                if(mSavingCounter > 0){
                    mSavingCounter--;
                } else {
                    //bmp.recycle();
                    VideoActivity.setMotionStatus(false);
                    VideoActivity.setNumberOfPerson(0);
                    return;
                }
            } else {
                mSavingCounter=PROCESS_FRAMES_AFTER_MOTION_DETECTED;
                mLastBigChangeTime = System.currentTimeMillis();
            }

            VideoActivity.setMotionStatus(true);

            try {
                file = screenshot.getInstance()
                        .saveScreenshotToPicturesFolder(mContext, bmp, "frame_");

                filename = file.getAbsolutePath();

            } catch (Exception e) {
                e.printStackTrace();
                //delete all jpg file in Download dir when disk is full
                deleteAllCapturedPics();
            }

            //bitmap.recycle();
            //bitmap = null;
            if(filename.equals("")){
                return;
            }
            if(file == null){
                return;
            }
            mBackgroundHandler.obtainMessage(PROCESS_SAVED_IMAGE_MSG, filename).sendToTarget();
            return;
        }
        private void processFrame(SurfaceTexture surface){
            long tsStart = System.currentTimeMillis();
            long tsEnd;
            tsEnd = System.currentTimeMillis();
            Bitmap bmp= mWeakRenderView.get().getBitmap();
            Log.v(TAG,"time diff (getBitmap) "+(tsEnd-tsStart));
            processBitmap(bmp);
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            long currentTime = System.currentTimeMillis();
            /*boolean needSaveFrame = false;
            if(mStartTime == 0) {
                mStartTime = currentTime;
                needSaveFrame = true;
            } else if (currentTime - mStartTime > 200){
                needSaveFrame = true;
                mStartTime = currentTime;
            }
            if(needSaveFrame){
                long start = System.currentTimeMillis();
                processFrame(surface);
                long end = System.currentTimeMillis();
                Log.v(TAG,"time diff (Total) "+(end-start));
                Log.v(TAG,"time diff (======================) ");
            }*/
            if (mFrameUpdateListener != null) {
                mFrameUpdateListener.onFrameUpdate(currentTime);
            }
        }

        //-------------------------
        // ISurfaceTextureHost
        //-------------------------

        @Override
        public void releaseSurfaceTexture(SurfaceTexture surfaceTexture) {
            if (surfaceTexture == null) {
                Log.d(TAG, "releaseSurfaceTexture: null");
            } else if (mDidDetachFromWindow) {
                if (surfaceTexture != mSurfaceTexture) {
                    Log.d(TAG, "releaseSurfaceTexture: didDetachFromWindow(): release different SurfaceTexture");
                    surfaceTexture.release();
                } else if (!mOwnSurfaceTexture) {
                    Log.d(TAG, "releaseSurfaceTexture: didDetachFromWindow(): release detached SurfaceTexture");
                    surfaceTexture.release();
                } else {
                    Log.d(TAG, "releaseSurfaceTexture: didDetachFromWindow(): already released by TextureView");
                }
            } else if (mWillDetachFromWindow) {
                if (surfaceTexture != mSurfaceTexture) {
                    Log.d(TAG, "releaseSurfaceTexture: willDetachFromWindow(): release different SurfaceTexture");
                    surfaceTexture.release();
                } else if (!mOwnSurfaceTexture) {
                    Log.d(TAG, "releaseSurfaceTexture: willDetachFromWindow(): re-attach SurfaceTexture to TextureView");
                    setOwnSurfaceTexture(true);
                } else {
                    Log.d(TAG, "releaseSurfaceTexture: willDetachFromWindow(): will released by TextureView");
                }
            } else {
                if (surfaceTexture != mSurfaceTexture) {
                    Log.d(TAG, "releaseSurfaceTexture: alive: release different SurfaceTexture");
                    surfaceTexture.release();
                } else if (!mOwnSurfaceTexture) {
                    Log.d(TAG, "releaseSurfaceTexture: alive: re-attach SurfaceTexture to TextureView");
                    setOwnSurfaceTexture(true);
                } else {
                    Log.d(TAG, "releaseSurfaceTexture: alive: will released by TextureView");
                }
            }
        }

        public void willDetachFromWindow() {
            Log.d(TAG, "willDetachFromWindow()");
            mWillDetachFromWindow = true;
        }

        public void didDetachFromWindow() {
            Log.d(TAG, "didDetachFromWindow()");
            mDidDetachFromWindow = true;
        }
    }

    public void processBitmap(Bitmap bmp){

        long tsStart = System.currentTimeMillis();
        long tsEnd;

        boolean bigChanged = mMotionDetection.detect(bmp);
        tsEnd = System.currentTimeMillis();

        Log.v(TAG,"time diff (motion) "+(tsEnd-tsStart));

        String filename = "";
        File file = null;
        VideoActivity.setPixelDiff(mMotionDetection.getPercentageOfDifferentPixels());

        // if no bigchange, but timespan between two uploaded frames is larger than 30s, treat it as big change
        if (!bigChanged) {
            long tm = System.currentTimeMillis();
            if (tm - mLastBigChangeTime > 30*1000) {
                bigChanged = true;
            }
        }
        if(!bigChanged){
            Log.d(TAG,"No Big changes, skip this frame");

            if(mSavingCounter > 0){
                mSavingCounter--;
            } else {
                //bmp.recycle();
                VideoActivity.setMotionStatus(false);
                VideoActivity.setNumberOfPerson(0);
                return;
            }
        } else {
            mSavingCounter=PROCESS_FRAMES_AFTER_MOTION_DETECTED;
            mLastBigChangeTime = System.currentTimeMillis();
        }

        VideoActivity.setMotionStatus(true);

        tsStart = System.currentTimeMillis();
        Bitmap original = bmp.copy(bmp.getConfig(), true);
        tsEnd = System.currentTimeMillis();
        Log.v(TAG,"time diff (bmp.copy) "+(tsEnd-tsStart));

        tsStart = System.currentTimeMillis();
        List<Classifier.Recognition> result =  mDetector.processImage(bmp);
        tsEnd = System.currentTimeMillis();
        Log.v(TAG,"time diff (OD) "+(tsEnd-tsStart));

        for(final Classifier.Recognition recognition:result){
            tsStart = System.currentTimeMillis();
            RectF rectf = recognition.getLocation();
            Log.d(TAG,"recognition rect: "+rectf.toString());
            Bitmap personBmp = getCropBitmapByCPU(bmp,rectf);
            mFaceDetector.predict_image(personBmp);
            tsEnd = System.currentTimeMillis();
            Log.v(TAG,"time diff (FD) "+(tsEnd-tsStart));
        }
        int personNum = result.size();
        VideoActivity.setNumberOfPerson(personNum);
        if (personNum>0){
            try {
                tsStart = System.currentTimeMillis();
                file = screenshot.getInstance()
                        .saveScreenshotToPicturesFolder(mContext, original, "frame_");

                filename = file.getAbsolutePath();
                tsEnd = System.currentTimeMillis();
                Log.v(TAG,"time diff (Save) "+(tsEnd-tsStart));

            } catch (Exception e) {
                e.printStackTrace();

                //delete all jpg file in Download dir when disk is full
                deleteAllCapturedPics();
            }
            //bitmap.recycle();
            //bitmap = null;
            if(filename.equals("")){
                return;
            }
            if(file == null){
                return;
            }
            mBackgroundHandler.obtainMessage(PROCESS_SAVED_IMAGE_MSG, filename).sendToTarget();
        }

        return;
    }
    private Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF) {
        Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(),
                (int) cropRectF.height(), Bitmap.Config.ARGB_8888);
        Canvas cavas = new Canvas(resultBitmap);

        // draw background
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        cavas.drawRect(/*www.j  a va2  s  .  co  m*/
                new RectF(0, 0, cropRectF.width(), cropRectF.height()),
                paint);

        Matrix matrix = new Matrix();
        matrix.postTranslate(-cropRectF.left, -cropRectF.top);

        cavas.drawBitmap(source, matrix, paint);

        if (source != null && !source.isRecycled()) {
            source.recycle();
        }

        return resultBitmap;
    }
    private Thread mDeletePicsThread = null;

    private void deleteAllCapturedPics() {
        if (mDeletePicsThread != null) {
            return;
        }

        mDeletePicsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File[] files = f.listFiles();

                    for (File fPic: files) {
                        if (fPic.isFile() && fPic.getPath().endsWith(".jpg")) {
                            fPic.delete();
                        }
                    }
                }
                catch (Exception ex) {}

                mDeletePicsThread = null;
            }
        });
        mDeletePicsThread.start();
    }

    //--------------------
    // Accessibility
    //--------------------

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(TextureRenderView.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(TextureRenderView.class.getName());
    }
}