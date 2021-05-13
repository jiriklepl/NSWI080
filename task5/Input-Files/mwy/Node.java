package mwy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Node {
	private int id;
	
	/* The set of nodes connected to this node by an edge. */
	private Set<Node> neighbors = new HashSet<Node>();

	public Node(int id) {
		this.id = id;
	}
	
	/** Gets the collection of nodes connected to this node by an edge. */
	public Collection<Node> getNeighbors() {
		return neighbors;
	}
	
	public void addNeighbor(Node neighbor) {
		neighbors.add(neighbor);
	}
}
