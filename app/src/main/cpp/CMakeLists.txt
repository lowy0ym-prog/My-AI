cmake_minimum_required(VERSION 3.22.1)
project(myai_llama_bridge)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# llama.cpp is fetched by CI into this directory before the Gradle build runs
# (see .github/workflows/android-build.yml). It is intentionally NOT vendored
# in this repo, to avoid committing a large third-party source tree.
set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

if(EXISTS ${LLAMA_CPP_DIR}/CMakeLists.txt)
    set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
    add_subdirectory(${LLAMA_CPP_DIR} llama_cpp_build)

    add_library(myai_llama_bridge SHARED llama_bridge.cpp)
    target_link_libraries(myai_llama_bridge llama log android)
    target_include_directories(myai_llama_bridge PRIVATE
        ${LLAMA_CPP_DIR}/include
        ${LLAMA_CPP_DIR}/ggml/include)
else()
    message(WARNING "llama.cpp sources not found at ${LLAMA_CPP_DIR} - building a stub bridge with no real inference.")
    add_library(myai_llama_bridge SHARED llama_bridge_stub.cpp)
    target_link_libraries(myai_llama_bridge log android)
endif()
