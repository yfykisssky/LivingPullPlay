LOCAL_PATH := $(call my-dir)
FAAD2_TOP := $(LOCAL_PATH)
include $(CLEAR_VARS)
include $(FAAD2_TOP)/libfaad/Android.mk

include $(CLEAR_VARS)
LOCAL_MODULE    := acctopcm
LOCAL_SRC_FILES := $(FAAD2_TOP)/acctopcm/native-interfaces.cpp
LOCAL_C_INCLUDES += $(FAAD2_TOP)/include/
LOCAL_LDLIBS +=  -llog -ldl -lz
LOCAL_SHARED_LIBRARIES := faad
include $(BUILD_SHARED_LIBRARY)