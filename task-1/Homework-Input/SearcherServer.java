import java.rmi.Naming;

public class SearcherServer
{
	public static void main(String[] args){
		try {
			// Instantiate the remotely accessible object. The constructor
			// of the object automatically exports it for remote invocation.
			Searcher searcher = new RemoteSearcherImpl();
			NodeFactory nodeFactory = new RemoteNodeFactoryImpl();
			
			// Use the registry on this host to register the server.
			// The host name must be changed if the server uses
			// another computer than the client!
			Naming.rebind(SearcherCommon.serverName, searcher);
			Naming.rebind(SearcherCommon.nodeFactoryName, nodeFactory);

			// The virtual machine will not exit here because the export of
			// the remotely accessible object creates a new thread that
			// keeps the application active.
		}
		catch (Exception e) {
			System.out.println("Server Exception: " + e.getMessage());
			e.printStackTrace();
		}
	} 
}
