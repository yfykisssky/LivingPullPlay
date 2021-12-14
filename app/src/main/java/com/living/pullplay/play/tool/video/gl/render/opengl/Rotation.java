package com.living.pullplay.play.tool.video.gl.render.opengl;

public enum Rotation {
    NORMAL, ROTATION_0,ROTATION_90, ROTATION_180, ROTATION_270;

    /**
     * Create a Rotation from an integer. Needs to be either 0, 90, 180 or 270.
     *
     * @param rotation 0, 90, 180 or 270
     * @return Rotation object
     */
    public static Rotation fromInt(int rotation) {
        switch (rotation) {
            case 90:
                return ROTATION_90;
            case 180:
                return ROTATION_180;
            case 270:
                return ROTATION_270;
            case 0:
                return ROTATION_0;
            case 360:
            default:
                return NORMAL;
        }
    }

    /**
     * Retrieves the int representation of the Rotation.
     *
     * @return 0, 90, 180 or 270
     */
    public int asInt() {
        switch (this) {
            case ROTATION_90:
                return 90;
            case ROTATION_180:
                return 180;
            case ROTATION_270:
                return 270;
            case NORMAL:
            case ROTATION_0:
            default:
                return 0;
        }
    }
}
