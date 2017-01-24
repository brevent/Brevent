cmake_minimum_required(VERSION 3.4.1)

add_library(reader SHARED src/main/cpp/log-reader.c )

add_library(log SHARED src/main/cpp/log-stub.c)

target_link_libraries(reader log)
