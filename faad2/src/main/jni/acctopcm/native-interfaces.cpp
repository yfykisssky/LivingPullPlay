#include <jni.h>

#include "acctopcm.h"
#include "acctopcm.cpp"
#include <android\log.h>
#include <jni.h>

unsigned char *as_unsigned_char_array(JNIEnv *env, jbyteArray array) {
    int len = env->GetArrayLength(array);
    auto *buf = new unsigned char[len];
    env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(buf));
    return buf;
}

jbyteArray as_byte_array(JNIEnv *env, unsigned char *buf, int len) {
    jbyteArray array = env->NewByteArray(len);
    env->SetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(buf));
    return array;
}

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
                                                     jbyteArray aac_bytes,
                                                     jint pcm_size) {

    int aacSize = env->GetArrayLength(aac_bytes);
    unsigned char pcm[pcm_size];
    unsigned char *aac = as_unsigned_char_array(env, aac_bytes);
    convertToPcm(aac, aacSize, pcm, pcm_size);
    return as_byte_array(env, pcm, pcm_size);

}




