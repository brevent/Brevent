
#ifndef _LIBS_LOG_LOG_H

#ifndef _LIBS_LOG_LOB_STUB_H
#define _LIBS_LOG_LOB_STUB_H

#include <stdio.h>

#ifndef UNUSED
#if defined(__GNUC__)
#define UNUSED(x) UNUSED_ ## x __attribute__((unused))
#endif
#endif

typedef enum log_id {
    LOG_ID_MIN = 0
} log_id_t;

typedef struct log_time {
    uint32_t tv_sec;
    uint32_t tv_nsec;
} __attribute__((__packed__)) log_time;

#define ANDROID_LOG_RDONLY O_RDONLY

#define LOGGER_ENTRY_MAX_LEN    (5*1024)

struct logger_entry {
    uint16_t    len;    /* length of the payload */
    uint16_t    __pad;  /* no matter what, we get 2 bytes of padding */
    int32_t     pid;    /* generating process's pid */
    int32_t     tid;    /* generating process's tid */
    int32_t     sec;    /* seconds since Epoch */
    int32_t     nsec;   /* nanoseconds */
    char        msg[0]; /* the entry's payload */
};

struct logger_entry_v4 {
    uint16_t len;
    /* length of the payload */
    uint16_t hdr_size;
    /* sizeof(struct logger_entry_v4) */
    int32_t pid;
    /* generating process's pid */
    uint32_t tid;
    /* generating process's tid */
    uint32_t sec;
    /* seconds since Epoch */
    uint32_t nsec;
    /* nanoseconds */
    uint32_t lid;
    /* log id of the payload, bottom 4 bits currently */
    uint32_t uid;       /* generating process's uid */
    char msg[0];    /* the entry's payload */
};

struct log_msg {
    union {
        struct logger_entry_v4 entry;
        struct logger_entry entry_v1;
        unsigned char buf[LOGGER_ENTRY_MAX_LEN + 1];
    } __attribute__((aligned(4)));
};

log_id_t android_name_to_log_id(const char *logName);

struct logger_list *android_logger_list_alloc_time(int mode,
                                                   log_time start,
                                                   pid_t pid);

struct logger *android_logger_open(struct logger_list *logger_list,
                                   log_id_t id);

int android_logger_list_read(struct logger_list *logger_list,
                             struct log_msg *log_msg);

void android_logger_list_free(struct logger_list *logger_list);

#endif
#endif
