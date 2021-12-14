/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.living.pullplay.play.tool.video.gl.render.opengl;

import static com.living.pullplay.play.tool.video.gl.render.opengl.GLConstants.TEXTURE_COORDS_NO_ROTATION;
import static com.living.pullplay.play.tool.video.gl.render.opengl.GLConstants.TEXTURE_COORDS_ROTATED_180;
import static com.living.pullplay.play.tool.video.gl.render.opengl.GLConstants.TEXTURE_COORDS_ROTATE_LEFT;
import static com.living.pullplay.play.tool.video.gl.render.opengl.GLConstants.TEXTURE_COORDS_ROTATE_RIGHT;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Pair;
import android.widget.ImageView.ScaleType;

import com.living.pullplay.utils.RecLogUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class OpenGlUtils {
    public static final int NO_TEXTURE = -1;
    public static final float[] CUBE = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
    public static final float[] TEXTURE = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};
    static final String TAG = "OpenGlUtils";

    public static int generateFrameBufferId() {
        int[] frameBufferIds = new int[1];
        GLES20.glGenFramebuffers(1, frameBufferIds, 0);
        return frameBufferIds[0];
    }

    public static FloatBuffer createNormalCubeVerticesBuffer() {
        return (FloatBuffer) ByteBuffer.allocateDirect(GLConstants.CUBE_VERTICES_ARRAYS.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(GLConstants.CUBE_VERTICES_ARRAYS)
            .position(0);
    }

    public static FloatBuffer createTextureCoordsBuffer(Rotation rotation, boolean flipHorizontal, boolean flipVertical) {
        float[] temp = new float[TEXTURE_COORDS_NO_ROTATION.length];
        initTextureCoordsBuffer(temp, rotation, flipHorizontal, flipVertical);

        FloatBuffer buffer = ByteBuffer.allocateDirect(TEXTURE_COORDS_NO_ROTATION.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        buffer.put(temp).position(0);
        return buffer;
    }

    public static void initTextureCoordsBuffer(float[] textureCoords, Rotation rotation,
                                               boolean flipHorizontal, boolean flipVertical) {
        float[] initRotation;
        switch (rotation) {
            case ROTATION_90:
                initRotation = TEXTURE_COORDS_ROTATE_RIGHT;
                break;
            case ROTATION_180:
                initRotation = TEXTURE_COORDS_ROTATED_180;
                break;
            case ROTATION_270:
                initRotation = TEXTURE_COORDS_ROTATE_LEFT;
                break;
            case NORMAL:
            default:
                initRotation = TEXTURE_COORDS_NO_ROTATION;
                break;
        }

        System.arraycopy(initRotation, 0, textureCoords, 0, initRotation.length);
        if (flipHorizontal) {
            textureCoords[0] = flip(textureCoords[0]);
            textureCoords[2] = flip(textureCoords[2]);
            textureCoords[4] = flip(textureCoords[4]);
            textureCoords[6] = flip(textureCoords[6]);
        }

        if (flipVertical) {
            textureCoords[1] = flip(textureCoords[1]);
            textureCoords[3] = flip(textureCoords[3]);
            textureCoords[5] = flip(textureCoords[5]);
            textureCoords[7] = flip(textureCoords[7]);
        }
    }

    private static float flip(final float i) {
        return i == 0.0f ? 1.0f : 0.0f;
    }

    public static int loadTexture(int format, Buffer data, int width, int height, int usedTexId) {
        int[] textures = new int[1];
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0);

            OpenGlUtils.bindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, data);
        } else {
            OpenGlUtils.bindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width, height, format, GLES20.GL_UNSIGNED_BYTE, data);
            textures[0] = usedTexId;
        }
        return textures[0];
    }

    public static int loadBitmapTexture(final Bitmap img, final int usedTexId) {
        if (img == null) {
            return NO_TEXTURE;
        }
        int textures[] = new int[1];
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0);
            textures[0] = usedTexId;
        }
        return textures[0];
    }

    public static int generateTextureOES() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    public static void deleteTexture(int textureId) {
        if (NO_TEXTURE == textureId) {
            return;
        }

        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
    }

    public static void deleteFrameBuffer(int frameBufferId) {
        if (NO_TEXTURE == frameBufferId) {
            return;
        }

        GLES20.glDeleteFramebuffers(1, new int[]{frameBufferId}, 0);
    }

    public static void bindTexture(int target, int texture) {
        GLES20.glBindTexture(target, texture);
        checkGlError("bindTexture");
    }

    public static void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            RecLogUtils.Companion.log(String.format("%s: glError %s", op, GLUtils.getEGLErrorString(error)));
        }
    }

    /**
     * 通过输入和输出的宽高，计算顶点数组和纹理数组
     *
     * @param scaleType          缩放方式，只能是{@link ScaleType#CENTER_CROP}和{@link ScaleType#CENTER}
     * @param inputRotation      输入纹理的旋转角度
     * @param needFlipHorizontal 是否进行镜面映射处理
     * @param inputWith          输入纹理的宽（未经处理的）
     * @param inputHeight        输入纹理的高（未经处理的）
     * @param outputWidth        绘制目标的宽
     * @param outputHeight       绘制目标的高
     * @return 返回顶点数组和纹理数组
     */
    public static Pair<float[], float[]> calcCubeAndTextureBuffer(ScaleType scaleType,
                                                                  Rotation inputRotation,
                                                                  boolean needFlipHorizontal,
                                                                  boolean needFlipVertical,
                                                                  int inputWith,
                                                                  int inputHeight,
                                                                  int outputWidth,
                                                                  int outputHeight) {

        boolean needRotate = (inputRotation == Rotation.ROTATION_90 || inputRotation == Rotation.ROTATION_270);
        int rotatedWidth = needRotate ? inputHeight : inputWith;
        int rotatedHeight = needRotate ? inputWith : inputHeight;
        float maxRratio = Math.max(1.0f * outputWidth / rotatedWidth, 1.0f * outputHeight / rotatedHeight);
        float ratioWidth = 1.0f * Math.round(rotatedWidth * maxRratio) / outputWidth;
        float ratioHeight = 1.0f * Math.round(rotatedHeight * maxRratio) / outputHeight;

        float[] cube = OpenGlUtils.CUBE;
        float[] textureCords = TextureRotationUtils.getRotation(inputRotation, needFlipHorizontal, needFlipVertical);
        if (scaleType == ScaleType.CENTER_CROP) {
            float distHorizontal = needRotate ? ((1 - 1 / ratioHeight) / 2) : ((1 - 1 / ratioWidth) / 2);
            float distVertical = needRotate ? ((1 - 1 / ratioWidth) / 2) : ((1 - 1 / ratioHeight) / 2);
            textureCords = new float[]{
                addDistance(textureCords[0], distHorizontal),
                addDistance(textureCords[1], distVertical),
                addDistance(textureCords[2], distHorizontal),
                addDistance(textureCords[3], distVertical),
                addDistance(textureCords[4], distHorizontal),
                addDistance(textureCords[5], distVertical),
                addDistance(textureCords[6], distHorizontal),
                addDistance(textureCords[7], distVertical),};
        } else {
            cube = new float[]{cube[0] / ratioHeight, cube[1] / ratioWidth,
                cube[2] / ratioHeight, cube[3] / ratioWidth,
                cube[4] / ratioHeight, cube[5] / ratioWidth,
                cube[6] / ratioHeight, cube[7] / ratioWidth,};
        }
        return new Pair<>(cube, textureCords);
    }

    private static float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }

}
