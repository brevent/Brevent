#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <fcntl.h>
#include <paths.h>
#include <dirent.h>
#include <time.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <android/log.h>

#define TAG "BreventLoader"
#define LOGD(...) (__android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__))
#define LOGW(...) (__android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__))
#define LOGE(...) (__android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__))

#define PROJECT "https://github.com/liudongmiao/Brevent/issues"

sig_atomic_t update;
sig_atomic_t quited;

#if defined(__aarch64__)
#define ABI "arm64"
#elif defined(__arm__)
#define ABI "arm"
#elif defined(__i386__)
#define ABI "x86"
#endif

static void rstrip(char *loader) {
    char *path;
    path = strchr(loader, '\r');
    if (path != NULL) {
        *path = '\0';
    }
    path = strchr(loader, '\n');
    if (path != NULL) {
        *path = '\0';
    }
}

static int worker() {
    FILE *file;
    char *path;
    char line[PATH_MAX];
    char libreader[PATH_MAX];
    char classpath[PATH_MAX];
    char *arg[] = {"app_process", NULL,
                   "/system/bin", "--nice-name=brevent_server",
                   "me.piebridge.brevent.server.BreventServer", NULL};
    file = popen("pm path me.piebridge.brevent", "r");
    if (file != NULL) {
        fgets(line, sizeof(line), file);
        rstrip(line);
        pclose(file);
    } else {
        return -1;
    }
    path = strchr(line, ':');
    if (path == NULL) {
        return -1;
    }
    path++;
    LOGD("loader path: %s", path);
    if (access(path, F_OK) == -1) {
        LOGE("can't find loader: %s", path);
        return -1;
    }
    memset(classpath, 0, PATH_MAX);
    sprintf(classpath, "CLASSPATH=%s", path);

    file = popen("dumpsys package me.piebridge.brevent", "r");
    if (file != NULL) {
        while (fgets(line, sizeof(line), file) != NULL) {
            rstrip(line);
            path = strstr(line, "legacyNativeLibraryDir=");
            if (path != NULL) {
                path = strchr(line, '=');
                path++;
                memset(libreader, 0, PATH_MAX);
                sprintf(libreader, "%s/" ABI "/libreader.so", path);
                LOGD("libreader: %s", libreader);
                if (access(libreader, F_OK) == -1) {
                    LOGW("can't find libreader: %s", libreader);
                }
                memset(libreader, 0, PATH_MAX);
                sprintf(libreader, "-Djava.libreader.path=%s/" ABI "/libreader.so", path);
                arg[1] = libreader;
                break;
            }
        }
        pclose(file);
    }
    pid_t pid = fork();
    switch (pid) {
        case -1:
            LOGE("cannot fork");
            return -1;
        case 0:
            break;
        default:
            return pid;
    }
    putenv(classpath);
    return execvp(arg[0], arg);
}

static void update_proc_title(size_t length, char *arg) {
    memset(arg, 0, length);
    strlcpy(arg, "brevent_daemon", length);
}

static int server(size_t length, char *arg) {
    sigset_t set;

    sigemptyset(&set);
    sigaddset(&set, SIGCHLD);

    if (sigprocmask(SIG_BLOCK, &set, NULL) == -1) {
        LOGE("cannot sigprocmask");
    }
    sigemptyset(&set);

    update_proc_title(length, arg);

    if (worker() <= 0) {
        return -EPERM;
    }

    for (;;) {
        sigsuspend(&set);
        if (quited) {
            LOGD("signal arrived, update: %d", update);
            if (!update) {
                break;
            }
            update = 0;
            quited = 0;
            if (worker() <= 0) {
                break;
            }
        }
    }

    return 0;
}

static void feedback() {
    printf("if you find any issues, please report bug to " PROJECT " with log\n"
                   "for crash log: logcat -b crash -d\n"
                   "for brevent log: logcat -b main -d -s BreventLoader BreventServer\n");
    fflush(stdout);
}

static void report(time_t now) {
    char command[BUFSIZ];
    char time[BUFSIZ];
    struct tm *tm = localtime(&now);
    strftime(time, sizeof(time), "%m-%d %H:%M:%S.000", tm);
    printf("please report bug to " PROJECT " with log below\n"
                   "--- crash start ---\n");
    fflush(stdout);
    sprintf(command, "logcat -b crash -t '%s' -d", time);
    printf(">>> %s\n", command);
    fflush(stdout);
    system(command);
    fflush(stdout);
    printf("--- crash end ---\n");
    printf("--- brevent start ---\n");
    fflush(stdout);
    sprintf(command, "logcat -b main -t '%s' -d -s BreventLoader BreventServer", time);
    printf(">>> %s\n", command);
    fflush(stdout);
    system(command);
    fflush(stdout);
    printf("--- brevent end ---\n");
    printf("cannot listen port, please report bug to " PROJECT " with log above\n");
    fflush(stdout);
}

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
            if (!strcasecmp(buf, "brevent_server")) {
                pid = id;
                break;
            }
        }
    }
    closedir(proc);
    return pid;
}

static int check(time_t now) {
    int pid = 0;
    printf("checking..");
    for (int i = 0; i < 10; ++i) {
        int id = get_pid();
        if (pid == 0 && id > 0) {
            printf("brevent_server started, pid: %d\n", id);
            printf("checking for stable.");
            i = 0;
            pid = id;
        } else if (pid > 0 && id == 0) {
            printf("quited\n\n");
            fflush(stdout);
            report(now);
            return EXIT_FAILURE;
        }
        printf(".");
        fflush(stdout);
        sleep(1);
    }
    if (pid > 0) {
        printf("ok\n\n");
        fflush(stdout);
        feedback();
        return EXIT_SUCCESS;
    } else {
        printf("cannot listen port\n");
        fflush(stdout);
        report(now);
        return EXIT_FAILURE;
    }
}

static void check_original() {
    int pid;
    if ((pid = get_pid()) > 0) {
        printf("found old brevent_server, pid: %d, killing\n", pid);
        kill(pid, SIGTERM);
        for (int i = 0; i < 3; ++i) {
            if (get_pid() > 0) {
                sleep(1);
                kill(pid, SIGKILL);
            } else {
                return;
            }
        }
        printf("cannot kill original brevent_server, pid: %d\n", pid);
        exit(EPERM);
    }
}

static void signal_handler(int signo) {
    if (signo == SIGCHLD) {
        pid_t pid;
        int status;
        for (;;) {
            pid = waitpid(-1, &status, WNOHANG);
            if (pid == 0 || pid == -1) {
                return;
            }
            quited = 1;
            update = 0;
            if (WIFEXITED(status)) {
                if (WEXITSTATUS(status) == 0) {
                    LOGD("worker %d exited with status %d", pid, WEXITSTATUS(status));
                    update = 1;
                } else {
                    LOGE("worker %d exited with status %d", pid, WEXITSTATUS(status));
                }
            } else if (WIFSIGNALED(status)) {
                LOGE("worker %d exited on signal %d", pid, WTERMSIG(status));
            }
        }
    }
}

static size_t compute(int argc, char **argv) {
    char *s = argv[0];
    char *e = argv[argc - 1];
    return (e - s) + strlen(argv[argc - 1]) + 1;
}

int main(int argc, char **argv) {
    int fd;
    struct timeval tv;

    check_original();

    signal(SIGCHLD, signal_handler);

    gettimeofday(&tv, NULL);
    switch (fork()) {
        case -1:
            perror("cannot fork");
            return -EPERM;
        case 0:
            break;
        default:
            _exit(check(tv.tv_sec));
    }

    if (setsid() == -1) {
        perror("cannot setsid");
        return -EPERM;
    }

    chdir("/");

    umask(0);

    if ((fd = open(_PATH_DEVNULL, O_RDWR)) == -1) {
        perror("cannot open " _PATH_DEVNULL);
        return -EPERM;
    }

    if (dup2(fd, STDIN_FILENO) == -1) {
        perror("cannot dup2(STDIN)");
        return -EPERM;
    }

    if (dup2(fd, STDOUT_FILENO) == -1) {
        perror("cannot dup2(STDOUT)");
        return -EPERM;
    }

    if (dup2(fd, STDERR_FILENO) == -1) {
        perror("cannot dup2(STDERR)");
        return -EPERM;
    }

    if (fd > STDERR_FILENO && close(fd) == -1) {
        perror("cannot close");
        return -EPERM;
    }

    return server(compute(argc, argv), argv[0]);
}
