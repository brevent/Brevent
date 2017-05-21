# path, abi64 should have been defined
if [ -z $path -o -z $abi64 ]; then
    echo "please specify path and abi64" >&2
    exit 1
fi

package=me.piebridge.brevent
brevent=/data/local/tmp/brevent
rm -rf $brevent
cp $path $brevent
if [ ! -f $brevent ]; then
    echo "/data/local/tmp is not writalbe" >&2
    exit 1
fi
chmod 0755 $brevent
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
