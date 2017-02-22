LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := bootstrap
LOCAL_SRC_FILES := bootstrap.c
include $(BUILD_EXECUTABLE)
