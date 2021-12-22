#ifndef STREAMLIVINGPUSH_TOOLS_H
#define STREAMLIVINGPUSH_TOOLS_H

#include <jni.h>
#include <android/log.h>
#ifndef  LOG_TAG
#define  LOG_TAG    "FAAD2"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#endif

#endif //STREAMLIVINGPUSH_TOOLS_H


unsigned char *as_unsigned_char_array(JNIEnv *env, jbyteArray array);

jbyteArray as_byte_array(JNIEnv *env, unsigned char *buf, int len);
