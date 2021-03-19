# for the step 1 of the homework assignment
# to be run from the root directory (no longer reliable as the implementation changed)
local_measurement() {
    for EDGES in $(seq 100)
    do
        EDGES=$((1000 * (10 * EDGES - 8) / 2))
        bash run-client 1000 $EDGES > "documentation/local_measurement_1000_$EDGES"
    done
}

# for the step 2 of the homework assignment
# to be run from the root directory 
remote_searcher() {
    bash run-server &
    to_kill=$!
    sleep 20
    for EDGES in $(seq 100)
    do
        EDGES=$((200 * (2 * EDGES - 1) / 2))
        bash run-client 200 $EDGES > "documentation/remote_searcher_200_$EDGES"
    done
    kill -9 $to_kill
}