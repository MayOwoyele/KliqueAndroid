cmake_minimum_required(VERSION 3.10)
project(KliqueFFmpeg)

# Specify the C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Locate the NDK-provided libraries
# Assuming your .so and headers are in src/main/jniLibs/<abi>/(lib*.so and include/)

# Add a library target for the native code
add_library(
        native-ffmpeg
        SHARED
        cpp/native-lib.cpp
)

find_library(log-lib log)
include_directories(
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/include
        ${CMAKE_CURRENT_SOURCE_DIR}/cpp
)
# Link the FFmpeg static/shared libraries
target_link_libraries(
        native-ffmpeg
        # FFmpeg modules
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/lib/libavformat.so
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/lib/libavcodec.so
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/lib/libavutil.so
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/lib/libswscale.so
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/lib/libswresample.so
        ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/lib/libavfilter.so
        # Linker flags
        ${log-lib}
)