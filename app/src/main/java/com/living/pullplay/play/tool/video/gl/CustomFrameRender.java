package com.living.pullplay.play.tool.video.gl;

import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import android.view.Surface;
import android.widget.ImageView.ScaleType;

import androidx.annotation.RequiresApi;

import com.living.pullplay.play.tool.video.gl.basic.Size;
import com.living.pullplay.play.tool.video.gl.render.EglCore;
import com.living.pullplay.play.tool.video.gl.render.opengl.GPUImageFilter;
import com.living.pullplay.play.tool.video.gl.render.opengl.OpenGlUtils;
import com.living.pullplay.play.tool.video.gl.render.opengl.Rotation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.CountDownLatch;

import kotlin.jvm.Synchronized;

/**
 * ## 视频帧渲染流程
 * 视频帧渲染采用了 texture，也就是 openGL 纹理的方案，这是 android 系统下性能最好的一种视频处理方案，具体流程如下：
 * <p>
 * 1. 构造函数：会创建一个{@link HandlerThread}线程，所有的OpenGL操作均在该线程进行。
 * <p>
 * 2. start()：传入一个系统TextureView（这个 View 需要加到 activity 的控件树上），用来显示渲染的结果。
 * <p>
 * 3. onSurfaceTextureAvailable(): TextureView 的 SurfaceTexture 已经准备好，将SurfaceTexture与
 * {@link }中的EGLContext（可为null）作为参数，
 * 生成一个新的EGLContext，SurfaceTexture也会作为此EGLContext的渲染目标。
 * <p>
 * 4. onRenderVideoFrame(): SDK 视频帧回调，在回调中可以拿到视频纹理ID和对应的 EGLContext。
 * 用这个 EGLContext 作为参数创建出来的新的 EGLContext，这样新的 EGLContext 就能访问SDK返回的纹理。
 * 然后会向HandlerThread发送一个渲染消息，用来渲染得到的视频纹理。
 * <p>
 * 5. renderInternal(): HandlerThread线程具体的渲染流程，将视频纹理渲染到 TextureView。
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class CustomFrameRender implements Handler.Callback, RenderFrameListener {
    public static final String TAG = "CustomFrameRender";

    protected static final int MSG_RENDER = 2;
    protected static final int MSG_DESTROY = 3;
    protected static final int MSG_DESTROY_CONTEXT = 4;
    protected static final int MSG_CLEAR_DRAW_VIEWPORT = 5;

    protected Size mSurfaceSize = new Size();
    private Size mLastInputSize = new Size();
    private Size mLastOutputSize = new Size();

    private boolean needFlipHorizontal = true;
    private boolean flipHorizontalLast = false;
    private boolean needFlipVertical = true;

    private Rotation useRotation = Rotation.ROTATION_180;

    private final HandlerThread mGLThread;
    protected final GLHandler mGLHandler;
    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;

    private EglCore mEglCore;
    private GPUImageFilter mNormalFilter;

    protected ScaleType useScaleType = ScaleType.CENTER;

    abstract Surface getRenderSurface();

    public void setIsFlipHorizontal(boolean flipHorizontal) {
        needFlipHorizontal = flipHorizontal;
    }

    public boolean isFlipHorizontal() {
        return needFlipHorizontal;
    }

    @Override
    public void onRenderVideoFrame(TextureVideoFrame frame) {
        GLES20.glFinish();
        mGLHandler.obtainMessage(MSG_RENDER, frame).sendToTarget();
    }

    public CustomFrameRender() {
        mGLCubeBuffer = ByteBuffer.allocateDirect(OpenGlUtils.CUBE.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLCubeBuffer.put(OpenGlUtils.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(OpenGlUtils.TEXTURE.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTextureBuffer.put(OpenGlUtils.TEXTURE).position(0);

        mGLThread = new HandlerThread(TAG);
        mGLThread.start();
        mGLHandler = new GLHandler(mGLThread.getLooper(), this);
    }

    public void stop() {
        needFlipHorizontal = false;
        mGLHandler.obtainMessage(MSG_DESTROY).sendToTarget();
    }

    @Synchronized
    private void initGlComponent(Object eglContext) {

        try {
            mEglCore = new EglCore((android.opengl.EGLContext) eglContext, getRenderSurface());
        } catch (Exception e) {
            return;
        }

        mEglCore.makeCurrent();
        mNormalFilter = new GPUImageFilter();
        mNormalFilter.init();
    }

    private void renderInternal(TextureVideoFrame frame) {

        if (mEglCore == null && getRenderSurface() != null) {
            Object eglContext = null;
            if (frame.getTextureId() != -1) {
                eglContext = frame.getEglContext14();
            }
            initGlComponent(eglContext);
        }

        if (mEglCore == null) {
            return;
        }

        if (mLastInputSize.width != frame.getWidth() || mLastInputSize.height != frame.getHeight()
            || mLastOutputSize.width != mSurfaceSize.width || mLastOutputSize.height != mSurfaceSize.height
            || flipHorizontalLast != needFlipHorizontal) {
            Pair<float[], float[]> cubeAndTextureBuffer = OpenGlUtils.calcCubeAndTextureBuffer(useScaleType,
                useRotation, needFlipHorizontal, needFlipVertical, frame.getWidth(), frame.getHeight(), mSurfaceSize.width, mSurfaceSize.height);
            mGLCubeBuffer.clear();
            mGLCubeBuffer.put(cubeAndTextureBuffer.first);
            mGLTextureBuffer.clear();
            mGLTextureBuffer.put(cubeAndTextureBuffer.second);

            mLastInputSize = new Size(frame.getWidth(), frame.getHeight());
            mLastOutputSize = new Size(mSurfaceSize.width, mSurfaceSize.height);

            flipHorizontalLast = needFlipHorizontal;
        }

        mEglCore.makeCurrent();
        GLES20.glViewport(0, 0, mSurfaceSize.width, mSurfaceSize.height);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        mNormalFilter.onDraw(frame.getTextureId(), mGLCubeBuffer, mGLTextureBuffer);

        mEglCore.setPresentationTime(frame.getCaptureTimeStamp());
        mEglCore.swapBuffer();
    }

    @Synchronized
    protected void uninitGlComponent() {

        if (mNormalFilter != null) {
            mNormalFilter.destroy();
            mNormalFilter = null;
        }
        if (mEglCore != null) {
            mEglCore.unmakeCurrent();
            mEglCore.destroy();
            mEglCore = null;
        }
    }

    private void destroyInternal() {
        uninitGlComponent();
        mGLHandler.getLooper().quitSafely();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_RENDER:
                renderInternal((TextureVideoFrame) msg.obj);
                break;
            case MSG_DESTROY:
                destroyInternal();
                break;
            case MSG_DESTROY_CONTEXT:
                uninitGlComponent();
                break;
            case MSG_CLEAR_DRAW_VIEWPORT:
                clearDrawViewport();
                break;
        }
        return false;
    }

    private void clearDrawViewport() {
        if (mEglCore != null) {
            mEglCore.makeCurrent();
            GLES20.glViewport(0, 0, mSurfaceSize.width, mSurfaceSize.height);
            GLES20.glClearColor(0, 0, 0, 0);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            mEglCore.swapBuffer();
        }
    }

    public static class GLHandler extends Handler {
        public GLHandler(Looper looper, Callback callback) {
            super(looper, callback);
        }

        public void runAndWaitDone(final Runnable runnable) {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            post(() -> {
                runnable.run();
                countDownLatch.countDown();
            });

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
