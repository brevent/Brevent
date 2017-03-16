cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "-Wall -Wextra -Werror")

add_library(reader SHARED src/main/cpp/log-reader.c )

add_library(log SHARED src/main/cpp/log-stub.c)

target_link_libraries(reader log)
