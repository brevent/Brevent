There is no source for `brevent-server`.

So, if you build your own `Brevent`, it won't work at all.
However, it's possible, as the `brevent-server` is compiled into `classes2.dex` in `brevent.apk`.

Simple instruction:

1. build the Brevent (UI) from source code

```
gradle clean :brevent:aR
```

2. get `classes2.dex` from `brevent.apk`

```
unzip /path/to/brevent.apk classes2.dex
```

3. update original apk

```
jar uf /path/to/builded.apk classes2.dex
jarsigner ...
```
