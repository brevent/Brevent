LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := log
LOCAL_CFLAGS += -Wall -Wextra -Werror
LOCAL_SRC_FILES := log-stub.c
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := reader
LOCAL_CFLAGS += -Wall -Wextra -Werror
LOCAL_SRC_FILES := log-reader.c
LOCAL_SHARED_LIBRARIES := log
include $(BUILD_SHARED_LIBRARY)
