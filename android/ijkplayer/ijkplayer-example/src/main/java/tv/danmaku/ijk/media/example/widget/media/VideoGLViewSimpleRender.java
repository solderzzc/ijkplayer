package tv.danmaku.ijk.media.example.widget.media;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * 在videffects的基础上调整的
 * <p>
 * 原 @author sheraz.khilji
 */
@SuppressLint("ViewConstructor")
public class VideoGLViewSimpleRender extends VideoGLViewBaseRender {
    private static final String TAG="GLSimpleRender";

    protected static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    private int mTextureID[] = new int[2];

    private boolean mUpdateSurface = false;

    private boolean mTakeShotPic = false;

    private SurfaceTexture mSurface;

    public VideoGLViewSimpleRender() {
        Matrix.setIdentityM(mSTMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        synchronized (this) {
            if (mUpdateSurface) {
                Log.d(TAG,"GL onDrawFrame");
                mSurface.updateTexImage();
                mSurface.getTransformMatrix(mSTMatrix);
                mUpdateSurface = false;
            } else {
                return;
            }
        }
        initDrawFrame();

        bindDrawFrameTexture();

        takeBitmap(glUnused);

        GLES20.glFinish();

    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        Log.d(TAG,"GL onSurfaceChanged");
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {

        Log.d(TAG,"onSurfaceCreated");
        GLES20.glGenTextures(2, mTextureID, 0);

        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID[0]);
        checkGlError("glBindTexture mTextureID");

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        mSurface = new SurfaceTexture(mTextureID[0]);
        mSurface.setOnFrameAvailableListener(this);

        Surface surface = new Surface(mSurface);
        SurfaceHolder surfaceHolder = (SurfaceHolder)surface;
    }

    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        Log.d(TAG,"GL onFrameAvailable");
        mUpdateSurface = true;
    }

    @Override
    public void releaseAll() {

    }

    protected void initDrawFrame() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT
                | GLES20.GL_COLOR_BUFFER_BIT);

        checkGlError("glUseProgram");
    }


    protected void bindDrawFrameTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID[0]);
    }


    protected void takeBitmap(GL10 glUnused) {
        if (mTakeShotPic) {
            mTakeShotPic = false;
            //if (mVideoShotListener != null) {
                Bitmap bitmap = createBitmapFromGLSurface(0, 0, mSurfaceView.getWidth(), mSurfaceView.getHeight(), glUnused);
                //mVideoShotListener.getBitmap(bitmap);
            //}
        }
    }
    /**
     * 打开截图
     */
    public void takeShotPic() {
        mTakeShotPic = true;
    }

}


