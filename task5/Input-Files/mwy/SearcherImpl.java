package mwy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SearcherImpl implements Searcher {
	private Map<Integer, Node> nodeMap;
	private int lastNodeId;
	
	public SearcherImpl() {
		nodeMap = new HashMap<Integer, Node>();
		lastNodeId = 0;
	}
	
	public int getDistance(int nodeFromId, int nodeToId) {
		Node nodeFrom = nodeMap.get(nodeFromId);
		Node nodeTo = nodeMap.get(nodeToId);
		
		// Implements a trivial distance measurement algorithm.
		// Starting from the source node, a set of visited nodes
		// is always extended by immediate neighbors of all visited
		// nodes, until the target node is visited or no node is left.
		
		// visited keeps the nodes visited in past steps.
		// boundary keeps the nodes visited in current step.
		Set<Node> visited = new HashSet<Node>();
		Set<Node> boundary = new HashSet<Node>();

		int distance = 0;

		// We start from the source node.
		boundary.add(nodeFrom);

		// Traverse the graph until finding the target node.
		while(!boundary.contains(nodeTo)) {
			// Not having anything to visit means the target node cannot be reached.
			if (boundary.isEmpty())
				return Searcher.DISTANCE_INFINITE;

			Set<Node> traversing = new HashSet<Node>();

			// Collect a set of immediate neighbors of nodes visited in current step.
			for (Node node: boundary) {
				traversing.addAll(node.getNeighbors());
			}
			
			// Nodes visited in current step become nodes visited in past steps.
			visited.addAll(boundary);
			// Out of immediate neighbors, consider only those not yet visited.
			traversing.removeAll(visited);
			// Make these nodes the new nodes to be visited in current step.
			boundary = traversing;

			++distance;
		}

		return distance;
	}

	@Override
	public int addNode() {
		int id = lastNodeId++;
		nodeMap.put(id, new Node(id));
		return id;
	}

	@Override
	public void connectNodes(int nodeFromId, int nodeToId) {
		Node nodeFrom = nodeMap.get(nodeFromId);
		Node nodeTo = nodeMap.get(nodeToId);

		nodeFrom.addNeighbor(nodeTo);
	}
	
}
