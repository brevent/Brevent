#include <stdio.h>
#include "log.h"

#if __ANDROID_USE_LIBLOG_CLOCK_INTERFACE
clockid_t android_log_clockid() {
    return 0;
}
#endif

struct logger_list *android_logger_list_alloc_time(int, log_time, pid_t) {
    return NULL;
}

struct logger *android_logger_open(struct logger_list *, log_id_t) {
    return NULL;
}

int android_logger_list_read(struct logger_list *, struct log_msg *) {
    return 0;
}

void android_logger_list_free(struct logger_list *) {

}

int __android_log_print(int, const char *, const char *, ...) {
    return 0;
}
