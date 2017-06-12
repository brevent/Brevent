cmake_minimum_required(VERSION 3.4.1)

if (ANDROID_ABI STREQUAL "arm64-v8a")
    set(ANDROID_ARCH "arm64")
elseif (ANDROID_ABI STREQUAL "armeabi")
    set(ANDROID_ARCH "arm")
else()
    set(ANDROID_ARCH "${ANDROID_ABI}")
endif()

set(CMAKE_C_FLAGS "-Wall -Wextra -Werror -I${ANDROID_NDK}/platforms/${ANDROID_PLATFORM}/arch-${ANDROID_ARCH}/usr/include")

add_library(reader SHARED src/main/cpp/log-reader.c )

add_library(log SHARED src/main/cpp/log-stub.c)

target_link_libraries(reader log)
