LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include ../../../../../OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

OPENCV_LIB_TYPE  := STATIC
LOCAL_SRC_FILES  := DetectionBasedTracker.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -llog -ldl

LOCAL_MODULE     := detection_based_tracker

include $(BUILD_SHARED_LIBRARY)
