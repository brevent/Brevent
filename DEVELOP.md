## English 

There is no source for `brevent-server`.

So, if you build your own `Brevent`, it won't work at all.
However, it's possible, as the `brevent-server` is compiled into `classes2.dex` in `brevent.apk`.

Simple instruction:

1. build the Brevent (UI) from source code

```
gradle clean
gradle :brevent:aR
```

2. get `classes2.dex` from `brevent.apk`

```
unzip /path/to/brevent.apk classes2.dex
```

3. update original apk

```
jar uf ce.apk classes2.dex
```

4. apksigner

```
apksigner sign --key testkey.pk8 --cert testkey.x509.pem ce.apk
```

You can find `testkey.pk8` and `testkey.x509.pem` [here](https://github.com/android/platform_build/blob/master/target/product/security/).

## Polski

W tym repozytorium nie ma źródła dla `brevent-server`.

Zatem jeśli zbudujesz własny pakiet `Brevent`, nie będzie on mimo wszystko działał.
Jednakże jest to możliwe, gdyż `brevent-server` został skompilowany w `classes2.dex` w `brevent.apk`.

Prosta instrukcja:

1. Zbuduj Brevent (UI) z kodu źródłowego

```
gradle clean
gradle :brevent:aR
```

2. Wydobądź `classes2.dex` z `brevent.apk`

```
unzip /path/to/brevent.apk classes2.dex
```

3. Zaktualizuj oryginalny plik apk

```
jar uf ce.apk classes2.dex
```

4. Apksigner

```
apksigner sign --key testkey.pk8 --cert testkey.x509.pem ce.apk
```

[Tutaj](https://github.com/android/platform_build/blob/master/target/product/security/) możesz znaleźć `testkey.pk8` oraz `testkey.x509.pem`.
