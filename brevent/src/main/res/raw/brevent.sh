#!/system/bin/sh

package=me.piebridge.brevent
brevent=/data/local/tmp/brevent
lnld=`dumpsys package $package | grep legacyNativeLibraryDir`
if [ x"$lnld" == x"" ]; then
    echo "please install $package" >&2
    exit 1
else
    lib=`echo $lnld | dd bs=1 skip=23 2>/dev/null`
    path=`ls $lib/*/libbrevent.so`
    if [ ! -f "$path" ]; then
        echo "please install latest $package" >&2
        exit 1
    else
        rm -rf $brevent
        cp $path $brevent
        if echo $path | grep -q -v 64; then
            rm -rf /data/local/tmp/app_process
            if [ -x /system/bin/app_process32 ]; then
                ln -s /system/bin/app_process32 /data/local/tmp/app_process
                export PATH=/data/local/tmp:$PATH
            fi
        fi
        exec $brevent
        exit 0
    fi
fi
