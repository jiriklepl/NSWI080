public class NodeFactoryImpl implements NodeFactory {
    @Override
    public Node createNode() {
        return new NodeImpl();
    }
}
