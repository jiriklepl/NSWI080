import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;

public class SearcherClient {
	// How many nodes and how many edges to create.
	private static int GRAPH_NODES;
	private static int GRAPH_EDGES;

	// How many searches to perform
	private static final int SEARCHES = 50;

	private static Node[] nodes;

	private static Random random = new Random();
	private static Searcher localSearcher = null;
	private static Searcher remoteSearcher = null;

	/**
	 * Creates nodes of a graph.
	 * 
	 * @param howMany number of nodes
	 */
	public static void createNodes(int howMany) {
		nodes = new Node[howMany];

		for (int i = 0; i < howMany; i++) {
			nodes[i] = new NodeImpl();
		}
	}

	/**
	 * Creates a fully connected graph.
	 */
	public static void connectAllNodes() {
		for (int idxFrom = 0; idxFrom < nodes.length; idxFrom++) {
			for (int idxTo = idxFrom + 1; idxTo < nodes.length; idxTo++) {
				nodes[idxFrom].addNeighbor(nodes[idxTo]);
				nodes[idxTo].addNeighbor(nodes[idxFrom]);
			}
		}
	}

	/**
	 * Creates a randomly connected graph.
	 * 
	 * @param howMany number of edges
	 */
	public static void connectSomeNodes(int howMany) {
		for (int i = 0; i < howMany; i++) {
			final int idxFrom = random.nextInt(nodes.length);
			final int idxTo = random.nextInt(nodes.length);

			nodes[idxFrom].addNeighbor(nodes[idxTo]);
		}
	}

	/**
	 * Runs a quick measurement on the graph.
	 * 
	 * @param howMany number of measurements
	 */
	public static void searchBenchmark(int howMany) throws RemoteException {
		// Display measurement header.
		System.out.printf("%7s %8s %13s %13s %13s %13s%n", "Attempt", "Distance", "Time", "TTime", "RTime", "RTTime");
		for (int i = 0; i < howMany; i++) {
			// Select two random nodes.
			final int idxFrom = random.nextInt(nodes.length);
			final int idxTo = random.nextInt(nodes.length);

			// Calculate distance, measure operation time
			final long localStartTimeNs = System.nanoTime();
			final int distance = localSearcher.getDistance(nodes[idxFrom], nodes[idxTo]);
			final long localDurationNs = System.nanoTime() - localStartTimeNs;

			// Calculate distance, measure operation time
			final long remoteStartTimeNs = System.nanoTime();
			if (distance != remoteSearcher.getDistance(nodes[idxFrom], nodes[idxTo]))
				throw new RemoteException();
			final long remoteDurationNs = System.nanoTime() - remoteStartTimeNs;

			// Calculate transitive distance, measure operation time
			final long localStartTimeTransitiveNs = System.nanoTime();
			final int distanceTransitive = localSearcher.getDistanceTransitive(4, nodes[idxFrom], nodes[idxTo]);
			final long localDurationTransitiveNs = System.nanoTime() - localStartTimeTransitiveNs;

			// Calculate transitive distance, measure operation time
			final long remoteStartTimeTransitiveNs = System.nanoTime();
			if (distanceTransitive != remoteSearcher.getDistanceTransitive(4, nodes[idxFrom], nodes[idxTo]))
				throw new RemoteException();
			final long remoteDurationTransitiveNs = System.nanoTime() - remoteStartTimeTransitiveNs;

			if (distance != distanceTransitive) {
				System.out.printf("Standard and transitive algorithms inconsistent (%d != %d)%n", distance,
						distanceTransitive);
			} else {
				// Print the measurement result.
				System.out.printf("%7d %8d %13d %13d %13d %13d%n", i, distance, localDurationNs / 1000, localDurationTransitiveNs / 1000, remoteDurationNs / 1000, remoteDurationTransitiveNs / 1000);
			}
		}
	}

	public static void main(String[] args) {
		// Create a randomly connected graph and do a quick measurement.
		// Consider replacing connectSomeNodes with connectAllNodes to verify that all distances are equal to one.
		if (args.length == 2) {
			try {
				GRAPH_NODES = Integer.parseInt(args[0]);
				GRAPH_EDGES = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.printf("Bad format%n");
				System.exit(1);
			}
		} else if (args.length == 0) {
			GRAPH_NODES = 1000;
			GRAPH_EDGES = 2000;
		} else {
			System.err.printf("Bad format%n");
			System.exit(1);
		}

		try {
			localSearcher = new SearcherImpl();
			remoteSearcher = (Searcher)Naming.lookup("//localhost/SearcherServer");
			createNodes(GRAPH_NODES);
			connectSomeNodes(GRAPH_EDGES);
			searchBenchmark(SEARCHES);
		} catch (RemoteException | NotBoundException | MalformedURLException e) {
			System.out.println("Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
