package com.living.pullplay.play.tool.video.gl;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;

import com.living.pullplay.play.tool.video.gl.basic.Size;

public class ToSurfaceViewFrameRender extends CustomFrameRender {

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private TextureView mRenderView;
    private OnSurfaceSizeChanged sizeChangedListener;

    public ToSurfaceViewFrameRender() {
        useScaleType = ImageView.ScaleType.CENTER;
    }

    public interface OnSurfaceSizeChanged {
        void onSizeChanged(int width, int height);
    }

    public void setSurfaceSizeChangedListener(OnSurfaceSizeChanged sizeChangedListener) {
        this.sizeChangedListener = sizeChangedListener;
    }

    @Override
    Surface getRenderSurface() {
        if (mSurface == null && mSurfaceTexture != null) {
            mSurface = new Surface(mSurfaceTexture);
        }
        return mSurface;
    }

    private void callbackSizeChanged(int width, int height) {
        if (sizeChangedListener != null) {
            sizeChangedListener.onSizeChanged(width, height);
        }
    }

    public void updateRenderTextureView(TextureView textureView) {
        // 设置TextureView的SurfaceTexture生命周期回调，用于管理GLThread的创建和销毁
        mRenderView = textureView;
        mSurfaceTexture = mRenderView.getSurfaceTexture();

        if (mRenderView.isAvailable()) {
            int width = mRenderView.getWidth();
            int height = mRenderView.getHeight();
            mSurfaceSize = new Size(width, height);
            callbackSizeChanged(width, height);
        }

        mRenderView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                // 保存surfaceTexture，用于创建OpenGL线程
                mSurfaceTexture = surface;
                mSurfaceSize = new Size(width, height);
                callbackSizeChanged(width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                mSurfaceSize = new Size(width, height);
                callbackSizeChanged(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                // surface释放了，需要停止渲染
                mSurfaceTexture = null;
                // 等待Runnable执行完，再返回，否则GL线程会使用一个无效的SurfaceTexture
                mGLHandler.runAndWaitDone(() -> uninitGlComponent());
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    //是否清屏
    public void stop(boolean needClear) {
        sizeChangedListener = null;
        if (mRenderView != null) {
            mRenderView.setSurfaceTextureListener(null);
        }
        if(needClear){
            mGLHandler.obtainMessage(MSG_CLEAR_DRAW_VIEWPORT).sendToTarget();
        }
        super.stop();
    }

}
