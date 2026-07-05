cmake_minimum_required(VERSION 3.22.1)

project("scamdetectorapp")

add_library(
        scamdetectorapp
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        scamdetectorapp
        ${log-lib})

# Enable 16 KB ELF alignment for Android 15+ compatibility
target_link_options(scamdetectorapp PRIVATE "-Wl,-z,max-page-size=16384")

# 將 Gradle 傳入的 API_KEY 定義為 C++ 的 Preprocessor Definition
if(NOT DEFINED API_KEY)
    set(API_KEY "")
endif()
# 使用跳脫引號 \" 包裹 Key，確保 C++ 編譯器將其視為字串常值
target_compile_definitions(scamdetectorapp PRIVATE API_KEY=\"${API_KEY}\")
