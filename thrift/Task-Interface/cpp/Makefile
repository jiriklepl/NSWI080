CXX := g++
CXXOPTS := -std=c++20 -O3 -Wall
INCLUDES := gen-cpp
LIBS := thrift
BIN := server

gen-cpp-files := gen-cpp/Login.cpp gen-cpp/Reports.cpp gen-cpp/Search.cpp gen-cpp/Task_constants.cpp gen-cpp/Task_types.cpp
gen-cpp-objects := $(patsubst %.cpp,%.o,$(gen-cpp-files))


.PHONY: all

all: server

# Compile server from main source and generated sources
$(BIN): server.o $(gen-cpp-objects)
	$(CXX) $(CXXOPTS) -o $@ $(patsubst %,-I%,$(INCLUDES)) $^ $(patsubst %,-l%,$(LIBS))

# Generate source files from Thrift IDL
$(gen-cpp-files): Task.thrift
	thrift --gen cpp Task.thrift

%.o: %.cpp $(gen-cpp-files)
	$(CXX) $(CXXOPTS) -c -o $@ $(patsubst %,-I%,$(INCLUDES)) $<

clean:
	rm -f server
	rm -f server.o
	rm -rf gen-cpp


.PHONY: all clean