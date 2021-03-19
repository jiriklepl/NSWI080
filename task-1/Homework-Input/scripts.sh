# for the step 1 of the homework assignment
# no longer reliable as the implementation changed
local_measurement() {
    for EDGES in $(seq 100)
    do
        EDGES=$((1000 * (10 * EDGES - 8) / 2))
        if bash run-client 1000 $EDGES > "documentation/local_measurement_1000_$EDGES"
        then :
        else
            kill -9 $to_kill
            return 1
        fi
    done
}

# for the step 2 of the homework assignment
# no longer reliable as the implementation changed
remote_searcher() {
    CLASSPATH=.
    java -Djava.rmi.server.hostname=localhost SearcherServer &
    to_kill=$!
    sleep 20
    for EDGES in $(seq 100)
    do
        EDGES=$((200 * (2 * EDGES - 1) / 2))
        bash run-client 200 $EDGES > "documentation/remote_searcher_200_$EDGES"
    done
    kill -9 $to_kill
}

# for the step 3 of the homework assignment
remote_nodes() {
    CLASSPATH=.
    java -Djava.rmi.server.hostname=localhost SearcherServer &
    to_kill=$!
    sleep 5
    for EDGES in $(seq 100)
    do
        EDGES=$((200 * (2 * EDGES - 1) / 2))
        if bash run-client 200 $EDGES > "documentation/remote_nodes_200_$EDGES"
        then :
        else
            kill -9 $to_kill
            return 1
        fi
    done
    kill -9 $to_kill
}

# for the step 3 of the homework assignment
remote_nodes_and_searcher() {
    CLASSPATH=.
    java -Djava.rmi.server.hostname=localhost SearcherServer &
    to_kill=$!
    sleep 5
    for EDGES in $(seq 100)
    do
        EDGES=$((200 * (2 * EDGES - 1) / 2))
        if bash run-client 200 $EDGES > "documentation/remote_nodes_and_searcher_200_$EDGES"
        then :
        else
            kill -9 $to_kill
            return 1
        fi
    done
    kill -9 $to_kill
}