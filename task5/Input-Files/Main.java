import java.util.Random;

import mwy.Searcher;

public class Main {
	// How many nodes and how many edges to create.
	private static final int GRAPH_NODES = 1000;
	private static final int GRAPH_EDGES = 2000;

	// How many searches to perform
	private static final int SEARCHES = 50;
	
	private static Random random = new Random();
	private static Searcher searcher;
	
	private static int[] nodeIds;
	
	/**
	 * Creates nodes of a graph.
	 * @param nodeCount number of nodes to be created
	 */
	public static void createNodes(int nodeCount) {
		nodeIds = new int[nodeCount];
		
		for (int i = 0; i < nodeCount; ++i) {
			int id = searcher.addNode();
			nodeIds[i] = id;
		}
	}
	
	/**
	 * Creates a randomly connected graph.
	 * @param edgeCount number of edges to be added
	 */
	public static void connectSomeNodes(int edgeCount) {
		for (int i = 0; i < edgeCount; ++i) {
			int nodeFromId = nodeIds[random.nextInt(nodeIds.length)];
			int nodeToId = nodeIds[random.nextInt(nodeIds.length)];

			searcher.connectNodes(nodeFromId, nodeToId);
		}
	}
	
	/**
	 * Runs a quick measurement on the graph.
	 * @param attemptCount number of searches
	 */
	public static void searchBenchmark(int attemptCount) {
		// Display measurement header.
		System.out.printf("%7s %8s %13s%n", "Attempt", "Distance", "Time");
		for (int i = 0; i < attemptCount; ++i) {
			// Select two random nodes.
			int nodeFromId = nodeIds[random.nextInt(nodeIds.length)];
			int nodeToId = nodeIds[random.nextInt(nodeIds.length)];

			// Calculate distance, timing the operation.
			long time = System.nanoTime();
			int distance = searcher.getDistance(nodeFromId, nodeToId);
			time = System.nanoTime() - time;
			
			// Print the measurement result.
			System.out.printf("%7d %8d %13d%n", i, distance, time / 1000);
		}        
	}

	public static void main(String[] args) {
		// Create a randomly connected graph and do a quick measurement.
		// Consider replacing connectSomeNodes with connectAllNodes to
		// verify that all distances are equal to one.
		searcher = new mwy.SearcherImpl();
		createNodes(GRAPH_NODES);
		connectSomeNodes(GRAPH_EDGES);
		searchBenchmark(SEARCHES);
	}
}
