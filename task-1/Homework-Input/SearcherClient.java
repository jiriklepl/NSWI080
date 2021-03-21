import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SearcherClient {
	// How many nodes and how many edges to create.
	private static int GRAPH_NODES;
	private static int GRAPH_EDGES;

	// How many searches to perform
	private static final int SEARCHES = 50;

	private static Random random = new Random();
	/**
	 * Creates nodes of a graph.
	 * 
	 * @param howMany number of nodes
	 */
	public static Node[] createNodes(int howMany, NodeFactory nodeFactory) throws RemoteException {
		Node[] graph = new Node[howMany];

		for (int i = 0; i < howMany; i++) {
			graph[i] = nodeFactory.createNode();
		}

		return graph;
	}

	/**
	 * Creates a fully connected graph.
	 */
	public static void connectAllNodes(Node[] graph) throws RemoteException {
		for (int idxFrom = 0; idxFrom < graph.length; idxFrom++) {
			for (int idxTo = idxFrom + 1; idxTo < graph.length; idxTo++) {
				graph[idxFrom].addNeighbor(graph[idxTo]);
				graph[idxTo].addNeighbor(graph[idxFrom]);
			}
		}
	}

	/**
	 * Creates randomly connected graphs.
	 * 
	 * @param howMany number of edges
	 */
	public static void connectSomeNodes(int howMany, Node[][] graphs) throws RemoteException, InvalidParameterException {
		if (graphs.length == 0) {
			return;
		} 

		int length = graphs[0].length;
		
		for (int i = 1; i < graphs.length; i++) {
			if (graphs[i].length != length) {
				throw new InvalidParameterException("Inputted graphs differ in lengths.");
			}
		}
				
		for (int i = 0; i < howMany; i++) {
			final int idxFrom = random.nextInt(length);
			final int idxTo = random.nextInt(length);

			for (int j = 0; j < graphs.length; j++) {
				graphs[j][idxFrom].addNeighbor(graphs[j][idxTo]);
			}
		}
	}

	/**
	 * Runs a quick measurement on the graph.
	 * 
	 * @param howMany number of measurements
	 */
	public static List<int[]> searchBenchmark(int howMany, Node[] graph, Searcher searcher) throws RemoteException, InvalidParameterException {
		List<int[]> indices = new ArrayList<int[]>();
		for (int i = 0; i < howMany; i++) {
			final int idxFrom = random.nextInt(graph.length);
			final int idxTo = random.nextInt(graph.length);

			indices.add(new int[]{idxFrom, idxTo});
		}

		return searchBenchmark(indices, graph, searcher);
	}

		/**
		 * Runs a quick measurement on the graph.
		 * 
		 * @param howMany number of measurements
		 */
		public static List<int[]> searchBenchmark(List<int[]> indices, Node[] graph, Searcher searcher) throws RemoteException, InvalidParameterException {
		// Display measurement header.
		System.out.printf("%7s %8s %13s %13s%n", "Attempt", "Distance", "Time", "TTime");
		for (int i = 0; i < indices.size(); i++) {
			// Select two random nodes.
			final int idxFrom = indices.get(i)[0];
			final int idxTo = indices.get(i)[1];

			// Calculate distance, measure operation time
			final long startTimeNs = System.nanoTime();
			final int distance = searcher.getDistance(graph[idxFrom], graph[idxTo]);
			final long durationNs = System.nanoTime() - startTimeNs;

			// Calculate transitive distance, measure operation time
			final long startTimeTransitiveNs = System.nanoTime();
			final int distanceTransitive = searcher.getDistanceTransitive(5, graph[idxFrom], graph[idxTo]);
			final long durationTransitiveNs = System.nanoTime() - startTimeTransitiveNs;

			if (distance != distanceTransitive) {
				System.out.printf("Standard and transitive algorithms inconsistent (%d != %d)%n", distance,
						distanceTransitive);
			} else {
				// Print the measurement result.
				System.out.printf("%7d %8d %13d %13d%n", i, distance, durationNs / 1000, durationTransitiveNs / 1000);
			}
		}

		return indices;
	}
	public static void main(String[] args) {
		final String usage = "The correct arguments are: [<nodes> <edges> [[<host>] <local_nodes/remote_nodes> <local_searcher/remote_searcher>]%nIf not specified otherwise all will be assumed local";

		NodeFactory nodeFactory = null;
		Searcher searcher = null;
		String host = "localhost";

		try {
			if (args.length == 2 || args.length == 4 || args.length == 5) {
				try {
					GRAPH_NODES = Integer.parseInt(args[0]);
					GRAPH_EDGES = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					System.err.printf("Bad format for a number%n%s%n", usage);
					System.exit(1);
				}
				if (args.length == 4 || args.length == 5) {
					int host_offset = 0;
					if (args.length == 5) {
						host_offset = 1;
						host = args[2];
					}

					if (args[2 + host_offset].equals("remote_nodes")) {
						nodeFactory = (NodeFactory)Naming.lookup(SearcherCommon.nodeFactoryName(host));
					} else if (args[2 + host_offset].equals("local_nodes")) {
						nodeFactory = new NodeFactoryImpl();
					} else {
						System.err.printf("Bad format for nodes %n%s%n", usage);
						System.exit(1);
					}
					if (args[3 + host_offset].equals("remote_searcher")) {
						searcher = (Searcher)Naming.lookup(SearcherCommon.searcherName(host));
					} else if (args[3 + host_offset].equals("local_searcher")) {
						searcher = new SearcherImpl();
					} else {
						System.err.printf("Bad format for searcher %n%s%n", usage);
						System.exit(1);
					}
				}
			} else if (args.length == 0) {
				GRAPH_NODES = 1000;
				GRAPH_EDGES = 2000;
			} else {
				System.err.printf("Bad format for the number of arguments%n%s%n", usage);
				System.exit(1);
			}

			if (nodeFactory == null) {
				nodeFactory = new NodeFactoryImpl();
			}

			if (searcher == null) {
				searcher = new SearcherImpl();
			}

			Node[] nodes = createNodes(GRAPH_NODES, nodeFactory);
			connectSomeNodes(GRAPH_EDGES, new Node[][]{nodes});
			searchBenchmark(SEARCHES, nodes, searcher);
		} catch (RemoteException | NotBoundException | MalformedURLException e) {
			System.out.println("Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
