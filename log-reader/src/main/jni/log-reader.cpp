#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <jni.h>
#include <fcntl.h>
#include <dirent.h>
#include <stdlib.h>
#include <android/log.h>
#include <time.h>
#include <sys/stat.h>
#include <unistd.h>
#include "log.h"

#define TAG "BreventServer"
#define LOGW(...) (__android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__))
#define LOGI(...) (__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__))

#ifndef AID_SYSTEM
#define AID_SYSTEM 1000
#endif

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
        struct stat status;

        if (!(id = atoi(entry->d_name))) {
            continue;
        }

        sprintf(buf, "/proc/%u", id);
        memset(&status, 0, sizeof(struct stat));
        stat(buf, &status);
        if (status.st_uid != AID_SYSTEM) {
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

extern "C"
JNIEXPORT jint JNICALL
Java_me_piebridge_LogReader_getPid(JNIEnv *, jclass) {
    return get_pid();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_piebridge_LogReader_readEvents(JNIEnv *env, jclass, jint pid, jobject handler) {
    struct logger_list *logger_list;

    jclass eventClass = env->FindClass(ANDROID_UTIL_EVENT_LOG_EVENT);
    jmethodID eventConstructor = env->GetMethodID(eventClass, "<init>", "([B)V");

    jclass eventHandler = env->GetObjectClass(handler);
    jmethodID accept = env->GetMethodID(eventHandler, "accept", "(I)Z");
    jmethodID onEvent = env->GetMethodID(eventHandler, "onEvent",
                                         "(L" ANDROID_UTIL_EVENT_LOG_EVENT ";)V");

#if __ANDROID_USE_LIBLOG_CLOCK_INTERFACE
    log_time now(android_log_clockid());
#else
    log_time now(CLOCK_REALTIME);
#endif

    LOGI("system_server: %d, now: %u", pid, now.tv_sec);

    logger_list = android_logger_list_alloc_time(ANDROID_LOG_RDONLY, now, pid);
    if (!android_logger_open(logger_list, LOG_ID_EVENTS)) {
        LOGW("android_logger_open fail");
        android_logger_list_free(logger_list);
        return;
    }

    for (;;) {
        int32_t tag;
        struct log_msg log_msg;
        int size = android_logger_list_read(logger_list, &log_msg);

        if (size <= 0) {
            LOGW("android_logger_list_read, size: %d", size);
            break;
        }

        if (log_msg.id() != LOG_ID_EVENTS) {
            continue;
        }

        tag = *(int32_t *) log_msg.msg();
        if (env->CallBooleanMethod(handler, accept, tag)) {
            jsize len = size;
            jbyteArray array = env->NewByteArray(len);
            if (array == NULL) {
                LOGW("NewByteArray fail, len: %d", len);
                break;
            }

            jbyte *bytes = env->GetByteArrayElements(array, NULL);
            memcpy(bytes, log_msg.buf, (size_t) len);
            env->ReleaseByteArrayElements(array, bytes, 0);

            jobject event = env->NewObject(eventClass, eventConstructor, array);
            if (event == NULL) {
                LOGW("NewObject fail, len: %d", len);
                env->DeleteLocalRef(array);
                break;
            }
            env->CallVoidMethod(handler, onEvent, event);
            env->DeleteLocalRef(event);
            env->DeleteLocalRef(array);
        }
    }

    android_logger_list_free(logger_list);
}

static int killChild(int ppid, int deep) {
    int count = 0;
    DIR *proc;
    struct dirent *entry;

    if (!(proc = opendir("/proc"))) {
        return count;
    };

    while ((entry = readdir(proc))) {
        int pid;
        FILE *fp;
        char buf[PATH_MAX];

        if (!(pid = atoi(entry->d_name))) {
            continue;
        }

        sprintf(buf, "/proc/%u/status", pid);
        fp = fopen(buf, "r");
        if (fp != NULL) {
            while (fgets(buf, PATH_MAX - 1, fp) != NULL) {
                if (strncmp(buf, "PPid:", 0x5) == 0) {
                    char *tab = strchr(buf, '\t');
                    if (tab != NULL) {
                        if (atoi(tab + 1) == ppid) {
                            if (deep) {
                                count += killChild(pid, deep);
                            }
                            sprintf(buf, "/proc/%u/status", pid);
                            if (access(buf, F_OK) == -1) {
                                LOGI("exited %d (ppid %d)", pid, ppid);
                                count++;
                            } else if (kill(pid, SIGKILL) == 0) {
                                count++;
                                LOGI("killed %d (ppid %d)", pid, ppid);
                            } else {
                                LOGW("cannot kill %d (ppid %d): %s", pid, ppid, strerror(errno));
                            }
                        }
                    }
                    break;
                }
            }
            fclose(fp);
        }
    }
    closedir(proc);
    return count;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_piebridge_LogReader_killDescendants(JNIEnv *, jclass, jint pid) {
    return killChild(pid, 1);
}
