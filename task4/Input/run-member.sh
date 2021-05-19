#!/bin/sh
. ./setenv.sh
java -Dhazelcast.socket.bind.any=false Member "$@"
