#include <jni.h>

#include <faad.h>
#include <android\log.h>
#include <jni.h>

extern "C" JNIEXPORT void JNICALL
Java_com_living_faad2_Test_test(JNIEnv *env, jclass clazz) {
    __android_log_print(ANDROID_LOG_INFO, "xxx", "faac test success");
    NeAACDecHandle decoder = NeAACDecOpen();
    NeAACDecClose(decoder);
    __android_log_print(ANDROID_LOG_INFO, "xxx", "faad test success");
}