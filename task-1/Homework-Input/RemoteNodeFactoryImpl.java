import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteNodeFactoryImpl extends UnicastRemoteObject implements NodeFactory {
    private static final long serialVersionUID = 0L;

    protected RemoteNodeFactoryImpl() throws RemoteException {
        super();
    }

    @Override
    public Node createNode() throws RemoteException {
        return new RemoteNodeImpl();
    }
}
