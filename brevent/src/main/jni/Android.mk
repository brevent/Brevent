LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := brevent
LOCAL_CFLAGS += -Wall
LOCAL_SRC_FILES := brevent.c
LOCAL_LDLIBS := -llog
include $(BUILD_EXECUTABLE)
