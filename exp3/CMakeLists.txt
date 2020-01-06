cmake_minimum_required(VERSION 3.4.3)
project(z3experiment)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_LIST_DIR}/cmake")

find_package(LLVM REQUIRED CONFIG)
find_package(Z3 REQUIRED)

message(STATUS "Found LLVM ${LLVM_PACKAGE_VERSION}")
message(STATUS "Using LLVMConfig.cmake in: ${LLVM_DIR}")

set (CMAKE_CXX_STANDARD 14)

include_directories(${LLVM_INCLUDE_DIRS} ${Z3_INCLUDE_DIR})
add_definitions(${LLVM_DEFINITIONS})

# Now build our tools
add_executable(z3expr main.cpp)

# Find the libraries that correspond to the LLVM components
# that we wish to use
llvm_map_components_to_libnames(llvm_libs support core irreader)

# Link against LLVM libraries
target_link_libraries(z3expr ${llvm_libs} ${Z3_LIBRARIES})