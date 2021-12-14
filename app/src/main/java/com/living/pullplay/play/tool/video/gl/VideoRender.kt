package com.living.pullplay.play.tool.video.gl

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.EGLContext
import android.os.*
import android.view.Surface
import android.widget.ImageView
import com.living.pullplay.play.tool.video.gl.basic.FrameBuffer
import com.living.pullplay.play.tool.video.gl.render.EglCore
import com.living.pullplay.play.tool.video.gl.render.opengl.*
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*

class VideoRender : OnFrameAvailableListener {

    companion object {
        private const val MSG_KIND_INIT_RENDER = 0
        private const val MSG_KIND_RELEASE_RENDER = 1
        private const val MSG_KIND_UPDATE_RENDING = 2
    }

    private val TAG = this::class.java.simpleName

    private var mSurface: Surface? = null
    private var mSurfaceTexture: SurfaceTexture? = null
    private var mEglCore: EglCore? = null
    private var mFrameBuffer: FrameBuffer? = null
    private var mOesInputFilter: OesInputFilter? = null
    private var mGpuImageFilterGroup: GPUImageFilterGroup? = null
    private var mGLCubeBuffer: FloatBuffer? = null
    private var mGLTextureBuffer: FloatBuffer? = null
    private val mTextureTransform = FloatArray(16)
    private var mSurfaceTextureId = OpenGlUtils.NO_TEXTURE
    private var mRenderHandlerThread: HandlerThread? = null

    private var inputWidth = 0
    private var inputHeight = 0

    //异步返回数据
    private var createSurfaceLock = Object()

    fun updateFrameSize(inputWidth: Int, inputHeight: Int) {
        this.inputWidth = inputWidth
        this.inputHeight = inputHeight
    }

    interface VideoRenderCallBack {
        fun onDataCallBack(frame: TextureVideoFrame)
    }

    private var videoRenderCallBack: VideoRenderCallBack? = null
    fun setVideoRenderCallBack(videoRenderCallBack: VideoRenderCallBack?) {
        this.videoRenderCallBack = videoRenderCallBack
    }

    @Volatile
    private var mRenderHandler: RenderHandler? = null

    fun initRender(): Surface? {
        synchronized(createSurfaceLock) {
            mRenderHandlerThread = HandlerThread("RenderHandlerThread")
            mRenderHandlerThread?.start()
            mRenderHandler = mRenderHandlerThread?.looper?.let { RenderHandler(it, this) }
            mRenderHandler?.sendEmptyMessage(MSG_KIND_INIT_RENDER)
            createSurfaceLock.wait()
            return mSurface
        }
    }

    fun releaseRender() {
        mRenderHandler?.sendEmptyMessage(MSG_KIND_RELEASE_RENDER)
    }

    fun needCutFrame(): Boolean {
        return false
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        mRenderHandler?.sendEmptyMessage(MSG_KIND_UPDATE_RENDING)
    }

    fun onRender2DTextureFrame(textureFrame: TextureVideoFrame) {
        videoRenderCallBack?.onDataCallBack(textureFrame)
    }

    private fun updateTexture() {
        try {
            mSurfaceTexture?.let { sur ->

                sur.updateTexImage()

                if (needCutFrame()) {
                    return
                }

                sur.getTransformMatrix(mTextureTransform)
                mOesInputFilter?.setTexutreTransform(mTextureTransform)
                mGpuImageFilterGroup?.draw(
                    mSurfaceTextureId,
                    mFrameBuffer?.frameBufferId ?: 0,
                    mGLCubeBuffer,
                    mGLTextureBuffer
                )
                mEglCore?.swapBuffer()

                val textureFrame = TextureVideoFrame()
                textureFrame.eglContext14 = mEglCore?.eglContext as? EGLContext
                textureFrame.textureId = mFrameBuffer?.textureId ?: -1
                textureFrame.width = getTransOutWith()
                textureFrame.height = getTransOutHeight()
                textureFrame.captureTimeStamp = System.nanoTime()

                onRender2DTextureFrame(textureFrame)

            }
        } catch (e: Exception) {
            //Log.e(TAG, "onFrameAvailable: " + e.message, e)
        }
    }

    private class RenderHandler(looper: Looper, capture: VideoRender) : Handler(looper) {

        private val readerWeakReference = WeakReference(capture)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            readerWeakReference.get()?.let {
                when (msg.what) {
                    MSG_KIND_INIT_RENDER -> {
                        it.initGLRender()
                    }
                    MSG_KIND_RELEASE_RENDER -> {
                        it.releaseGLRender()
                    }
                    MSG_KIND_UPDATE_RENDING -> {
                        it.updateTexture()
                    }
                }
            }

        }

    }

    fun getInputWith(): Int {
        return inputWidth
    }

    fun getInputHeight(): Int {
        return inputHeight
    }

    fun getTransOutWith(): Int {
        return inputWidth
    }

    fun getTransOutHeight(): Int {
        return inputHeight
    }

    fun getOutRotation(): Rotation {
        return Rotation.NORMAL
    }

    fun needFlipHorizontal(): Boolean {
        return false
    }

    fun needFlipVertical(): Boolean {
        return false
    }

    private fun initGLRender() {

        val cubeAndTextureBuffer = OpenGlUtils.calcCubeAndTextureBuffer(
            ImageView.ScaleType.CENTER_CROP,
            getOutRotation(),
            needFlipHorizontal(),
            needFlipVertical(),
            getInputWith(),
            getInputHeight(),
            getTransOutWith(),
            getTransOutHeight()
        )

        mGLCubeBuffer =
            ByteBuffer.allocateDirect(OpenGlUtils.CUBE.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        mGLCubeBuffer?.put(cubeAndTextureBuffer.first)
        mGLTextureBuffer =
            ByteBuffer.allocateDirect(OpenGlUtils.TEXTURE.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        mGLTextureBuffer?.put(cubeAndTextureBuffer.second)

        mEglCore = EglCore(getInputWith(), getInputHeight())
        mEglCore?.makeCurrent()

        mSurfaceTextureId = OpenGlUtils.generateTextureOES()
        mSurfaceTexture = SurfaceTexture(mSurfaceTextureId)
        mSurfaceTexture?.setDefaultBufferSize(getInputWith(), getInputHeight())
        mSurfaceTexture?.setOnFrameAvailableListener(this@VideoRender)

        mSurface = Surface(mSurfaceTexture)

        mFrameBuffer = FrameBuffer(getInputWith(), getInputHeight())
        mFrameBuffer?.initialize()

        mGpuImageFilterGroup = GPUImageFilterGroup()
        mOesInputFilter = OesInputFilter()

        mGpuImageFilterGroup?.addFilter(mOesInputFilter)
        mGpuImageFilterGroup?.addFilter(GPUImageFilter(false))
        mGpuImageFilterGroup?.init()
        mGpuImageFilterGroup?.onOutputSizeChanged(getInputWith(), getInputHeight())

        synchronized(createSurfaceLock) {
            createSurfaceLock.notifyAll()
        }

    }

    fun releaseGLRender() {

        mRenderHandlerThread?.quit()

        mGpuImageFilterGroup?.destroy()
        mGpuImageFilterGroup = null

        mFrameBuffer?.uninitialize()
        mFrameBuffer = null

        if (mSurfaceTextureId != OpenGlUtils.NO_TEXTURE) {
            OpenGlUtils.deleteTexture(mSurfaceTextureId)
            mSurfaceTextureId = OpenGlUtils.NO_TEXTURE
        }

        mSurface?.release()
        mSurface = null

        mSurfaceTexture?.release()
        mSurfaceTexture = null

        mEglCore?.unmakeCurrent()
        mEglCore?.destroy()
        mEglCore = null

    }

}