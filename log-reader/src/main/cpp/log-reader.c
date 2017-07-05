#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <fcntl.h>
#include <dirent.h>
#include <stdlib.h>
#include <android/log.h>
#include <time.h>
#include "log.h"

#define TAG "BreventServer"
#define LOGW(...) (__android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__))
#define LOGI(...) (__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__))

#define ANDROID_UTIL_EVENT_LOG_EVENT "android/util/EventLog$Event"

static int get_pid() {
    int pid = 0;
    DIR *proc;
    struct dirent *entry;

    if (!(proc = opendir("/proc"))) {
        return pid;
    };

    while ((entry = readdir(proc))) {
        int id;
        FILE *fp;
        char buf[PATH_MAX];

        if (!(id = atoi(entry->d_name))) {
            continue;
        }
        sprintf(buf, "/proc/%u/cmdline", id);
        fp = fopen(buf, "r");
        if (fp != NULL) {
            fgets(buf, PATH_MAX - 1, fp);
            fclose(fp);
            if (!strcasecmp(buf, "system_server")) {
                pid = id;
                break;
            }
        }
    }
    closedir(proc);
    return pid;
}

/*
 * Class:     me_piebridge_LogReader
 * Method:    readEvents
 * Signature: (Lme/piebridge/EventHandler;)V
 */
JNIEXPORT void JNICALL
Java_me_piebridge_LogReader_readEvents(JNIEnv *env, jclass UNUSED(clazz), jobject value) {
    int pid;
    struct timespec ts;
    struct logger_list *logger_list;
    struct log_time log_time;

    jclass eventClass = (*env)->FindClass(env, ANDROID_UTIL_EVENT_LOG_EVENT);
    jmethodID eventConstructor = (*env)->GetMethodID(env, eventClass, "<init>", "([B)V");

    jclass eventHandler = (*env)->GetObjectClass(env, value);
    jmethodID accept = (*env)->GetMethodID(env, eventHandler, "accept", "(I)Z");
    jmethodID onEvent = (*env)->GetMethodID(env, eventHandler, "onEvent",
                                            "(L" ANDROID_UTIL_EVENT_LOG_EVENT ";)Z");

    pid = get_pid();
    clock_gettime(CLOCK_REALTIME, &ts);
    LOGI("system_server: %d, now: %ld", pid, ts.tv_sec);

    log_time.tv_sec = (uint32_t) ts.tv_sec;
    log_time.tv_nsec = (uint32_t) ts.tv_nsec;

    logger_list = android_logger_list_alloc_time(ANDROID_LOG_RDONLY, log_time, pid);
    if (!android_logger_open(logger_list, LOG_ID_EVENTS)) {
        return;
    }

    for (;;) {
        char *buf;
        int32_t tag;
        struct log_msg log_msg;
        int size = android_logger_list_read(logger_list, &log_msg);

        if (size == -EINTR) {
            continue;
        } else if (size <= 0) {
            break;
        }

        if (log_msg.entry.lid != LOG_ID_EVENTS) {
            continue;
        }

        buf = log_msg.entry.hdr_size ? (char *) log_msg.buf + log_msg.entry.hdr_size
                                     : log_msg.entry_v1.msg;
        tag = *(int32_t *) buf;
        if ((*env)->CallBooleanMethod(env, value, accept, tag)) {
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