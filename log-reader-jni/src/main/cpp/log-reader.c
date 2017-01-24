#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <fcntl.h>
#include <time.h>
#include "log.h"

#define ANDROID_UTIL_EVENT_LOG_EVENT "android/util/EventLog$Event"

static inline int32_t get_tag(struct log_msg msg) {
    char *buf;
    if (msg.entry.hdr_size) {
        buf = (char *) msg.buf + msg.entry.hdr_size;
    } else {
        buf = (char *) msg.entry_v1.msg;
    }
    return *((int32_t *) buf);
}

/*
 * Class:     me_piebridge_LogReader
 * Method:    readEvents
 * Signature: (ILme/piebridge/EventHandler;)V
 */
JNIEXPORT void JNICALL Java_me_piebridge_LogReader_readEvents(JNIEnv *env, jclass clazz, jint pid,
                                                              jobject value) {
    struct logger_list *logger_list;
    struct log_time log_time;

    jclass eventClass = (*env)->FindClass(env, ANDROID_UTIL_EVENT_LOG_EVENT);
    jmethodID eventConstructor = (*env)->GetMethodID(env, eventClass, "<init>", "([B)V");

    jclass eventHandler = (*env)->GetObjectClass(env, value);
    jmethodID accept = (*env)->GetMethodID(env, eventHandler, "accept", "(I)Z");
    jmethodID onEvent = (*env)->GetMethodID(env, eventHandler, "onEvent",
                                            "(L" ANDROID_UTIL_EVENT_LOG_EVENT ";)Z");

    struct timeval now;
    gettimeofday(&now, NULL);
    log_time.tv_sec = (uint32_t) now.tv_sec;
    log_time.tv_nsec = (uint32_t) (now.tv_usec / 1000);

    logger_list = android_logger_list_alloc_time(ANDROID_LOG_RDONLY, log_time, pid);
    android_logger_open(logger_list, android_name_to_log_id("events"));
    for (; ;) {
        struct log_msg log_msg;
        int size = android_logger_list_read(logger_list, &log_msg);
        if (size <= 0) {
            break;
        }

        if ((*env)->CallBooleanMethod(env, value, accept, get_tag(log_msg))) {
            jsize len = size;
            jbyteArray array = (*env)->NewByteArray(env, len);
            if (array == NULL) {
                break;
            }
            jbyte *bytes = (*env)->GetByteArrayElements(env, array, NULL);
            memcpy(bytes, log_msg.buf, (size_t) len);
            (*env)->ReleaseByteArrayElements(env, array, bytes, 0);
            jobject event = (*env)->NewObject(env, eventClass, eventConstructor, array);
            if (event == NULL) {
                (*env)->DeleteLocalRef(env, array);
                break;
            }
            jboolean result = (*env)->CallBooleanMethod(env, value, onEvent, event);
            (*env)->DeleteLocalRef(env, event);
            (*env)->DeleteLocalRef(env, array);
            if (!result) {
                break;
            }
        }
    }

    android_logger_list_free(logger_list);
}