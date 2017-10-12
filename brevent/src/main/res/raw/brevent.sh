# path, abi64 should have been defined
if [ -z $abi64 ]; then
    echo "ERROR: please open Brevent to make a new brevent.sh" >&2
    exit 1
fi

brevent=/data/local/tmp/brevent

# some os cannot execute $path directly
if [ -x $path ]; then
    if [ -f $brevent ]; then
        rm -rf $brevent
    fi
    if [ -f $brevent ]; then
        echo "WARNING: /data/local/tmp is not writable" >&2
    else
        cp $path $brevent
        if [ -f $brevent ]; then
            chmod 0755 $brevent
        else
            echo "WARNING: /data/local/tmp is not writable" >&2
        fi
    fi
elif [ ! -x $brevent ]; then
    echo "ERROR: please open Brevent to make a new brevent.sh" >&2
    exit 1
fi

# some os is 64bit, but load 32bit library(and binary)
if [ x"$abi64" == x"false" -a -x /system/bin/app_process64 ]; then
    rm -rf /data/local/tmp/app_process
    ln -s /system/bin/app_process32 /data/local/tmp/app_process
    export PATH=/data/local/tmp:$PATH
fi

if [ -x $brevent ]; then
    exec $brevent
elif [ -x $path ]; then
    exec $path
else
    echo "cannot execute brevent" >&2
    exit 1
fi
