# for the step 1 of the homework assignment
# to be run from the root directory
local_measurement() {
    for EDGES in $(seq 100)
    do
        EDGES=$((1000 * (10 * EDGES - 8) / 2))
        bash run-client 1000 $EDGES > "documentation/local_measurement_1000_$EDGES"
    done
}