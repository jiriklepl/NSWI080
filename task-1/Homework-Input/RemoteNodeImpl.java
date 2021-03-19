import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Set;

public class RemoteNodeImpl extends UnicastRemoteObject implements Node {
	private static final long serialVersionUID = 1L;
    private final NodeImpl implementation = new NodeImpl();

	public RemoteNodeImpl() throws RemoteException {
		super();
	}

    @Override
    public Set<Node> getNeighbors() throws RemoteException {
        return implementation.getNeighbors();
    }

    @Override
    public Map<Node, Integer> getTransitiveNeighbors(int distance) throws RemoteException {
        return implementation.getTransitiveNeighbors(distance);
    }

    @Override
    public void addNeighbor(Node neighbor) throws RemoteException {
        implementation.addNeighbor(neighbor);
    }

}