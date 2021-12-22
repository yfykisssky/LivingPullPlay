#include <jni.h>

#include "acctopcm.h"
#include "acctopcm.cpp"

#define MAX_CHANNEL_COUNTS 6
#define MAX_OUT_PCM_SIZE 1024*MAX_CHANNEL_COUNTS

extern "C"
JNIEXPORT void JNICALL
Java_com_living_faad2_AccFaad2NativeJni_startFaad2Engine(JNIEnv *env, jclass clazz, jint type,
                                                         jint fomt_bit_type, jlong sample_rate,
                                                         jint channels) {

    initDecoder(type, fomt_bit_type, sample_rate, channels);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_living_faad2_AccFaad2NativeJni_stopFaad2Engine(JNIEnv *env, jclass clazz) {
    unInitDecoder();
}
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_living_faad2_AccFaad2NativeJni_convertToPcm(JNIEnv *env, jclass clazz,
                                                     jbyteArray aac_bytes) {

    int aacSize = env->GetArrayLength(aac_bytes);
    unsigned char *aac = as_unsigned_char_array(env, aac_bytes);
    unsigned char pcmBytes[MAX_OUT_PCM_SIZE];
    int pcm_size = convertToPcm(aac, aacSize, pcmBytes);
    unsigned char pcmOutBytes[pcm_size];
    memcpy(pcmOutBytes, pcmBytes, pcm_size);
    return as_byte_array(env, pcmOutBytes, pcm_size);

}




