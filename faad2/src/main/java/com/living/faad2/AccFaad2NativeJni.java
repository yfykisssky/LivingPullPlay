package com.living.faad2;

class AccFaad2NativeJni {

    static {
        System.loadLibrary("acctopcm");
    }

    public native static void startFaad2Engine(int type, int fomtBitType, long sampleRate,int channels);

    public native static void stopFaad2Engine();

    public native static byte[] convertToPcm(byte[] aacBytes,int pcmSize);

}
