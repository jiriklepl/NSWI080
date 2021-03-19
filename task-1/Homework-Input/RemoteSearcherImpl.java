import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteSearcherImpl extends UnicastRemoteObject implements Searcher {
	private static final long serialVersionUID = 0L;

    private final SearcherImpl implementation = new SearcherImpl();

	public RemoteSearcherImpl() throws RemoteException {
		super();
	}

    @Override
    public int getDistance(Node from, Node to) throws RemoteException {
        return implementation.getDistance(from, to);
    }

    @Override
    public int getDistanceTransitive(int neighborDistance, Node from, Node to) throws RemoteException {
        return implementation.getDistanceTransitive(neighborDistance, from, to);
    }
    
}
