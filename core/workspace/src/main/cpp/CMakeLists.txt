cmake_minimum_required(VERSION 3.22.1)

project("workspace")

add_library(${CMAKE_PROJECT_NAME} SHARED
        workspace.cpp)

add_library(termux SHARED
        termux_pty.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log)

target_link_libraries(termux
        log)
