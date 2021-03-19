import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeFactory extends Remote {
    /**
     * Creates a new node
     * @return
     */
    public Node createNode() throws RemoteException;
}
