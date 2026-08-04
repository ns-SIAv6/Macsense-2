cmake_minimum_required(VERSION 3.22.1)
project("macsense_playback")

add_library(macsense_playback SHARED
    playback_engine.cpp
    playback_jni.cpp
)

find_library(log-lib log)
find_library(aaudio-lib aaudio)

target_link_libraries(macsense_playback
    ${log-lib}
    ${aaudio-lib}
)

target_compile_options(macsense_playback PRIVATE -Wall -Wextra -std=c++17)
