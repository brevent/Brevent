#!/system/bin/sh

function check() {
    echo -n "checking..."
    for x in 1 2 3 4 5 6 7 8 9 10; do
        sleep 1
        if netstat -tlnp 2>/dev/null | grep -q 59526; then
            echo "successfully" >&2
            exit 0
        fi
        echo -n "."
    done
    echo ""
    echo "cannot listen port, please report bug" >&2
    echo "--- crash start ---"
    logcat -b crash -t "$now"
    echo "--- crash end ---"
    echo "--- brevent start ---"
    logcat -b main -t "$now" -s BreventLoader BreventServer
    echo "--- brevent end ---"
    exit 3
}

name=`id -un`
if [ x"$name" = x"root" ]; then
    echo "please run with 'su shell $0'" >&2
    exit 0
fi

if [ x"$name" != x"shell" ]; then
    echo "cannot be run as $name" >&2
    exit 1
fi

dir=`dumpsys package me.piebridge.brevent | grep legacyNativeLibraryDir=`
if [ x"$dir" == x"" ]; then
    echo "cannot find brevent's legacyNativeLibraryDir" >&2
    exit 1
fi
dir=`echo $dir | dd bs=1 skip=23 2>/dev/null`
if getprop ro.product.cpu.abilist | grep -q x86; then
    abi=x86
else
    abi=arm
fi
lib="$dir/$abi/libloader.so"
if [ ! -f $lib ]; then
    echo "cannot find $lib" >&2
    exit 1
fi

netstat -tlnp 2>/dev/null | grep 59526 | while IFS=" /" read pr rq sq la fa st pid pn; do
    if [ ! -d /proc/$pid ]; then
        echo "cannot find pid $pid" >&2
        exit 2
    fi
    kill $pid
    if [ ! -d /proc/$pid ]; then
        echo "cannot kill $pid" >&2
        exit 2
    fi
done  

now=`date +'%s'`
export CLASSPATH=$lib
exec app_process32 /system/bin --nice-name=brevent_server me.piebridge.brevent.loader.Brevent "$@" &
check
exit $?
