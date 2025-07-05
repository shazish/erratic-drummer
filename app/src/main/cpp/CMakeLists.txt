cmake_minimum_required(VERSION 3.22.1)

project("native-lib")

# Find Oboe using Prefab
find_package(oboe REQUIRED CONFIG)

add_library(native-lib SHARED native-lib.cpp)

find_library(log-lib log)
find_library(android-lib android)
find_library(mediandk-lib mediandk)

target_link_libraries(native-lib
        ${log-lib}
        ${android-lib}
        ${mediandk-lib}
        oboe::oboe
)