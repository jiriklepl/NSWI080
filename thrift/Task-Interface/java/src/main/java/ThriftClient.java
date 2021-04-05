import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

// TODO: print step dump

public class ThriftClient {
    private static class Fields {
        private Map<String, Set<String>> fields = new TreeMap<String, Set<String>>();
        public final void addField(String fieldName, String fieldValue) {
            if (fields.containsKey(fieldName))
                fields.get(fieldName).add(fieldValue);
            else {
                Set<String> set = new TreeSet<String>();
                set.add(fieldValue);
                fields.put(fieldName, set);
            }
        };
    }
    public static void main(String args[]) {
        // Connect to server by TCP socket
        try (TTransport transport = new TSocket("localhost", 5000)) {
            // The socket transport is already buffered
            // Use a binary protocol to serialize data
            TProtocol muxProtocol = new TBinaryProtocol(transport);
            // Use a multiplexed protocol to select a service by name
            TProtocol loginProtocol = new TMultiplexedProtocol(muxProtocol, "Login");
            // Proxy object

            Login.Client loginClient = new Login.Client(loginProtocol);

            // Open the connection
            transport.open();

            int expectedKey = 0;
            int searchLimit = 100;
            String name = "name";
            String query = "itemA,itemB,itemC,itemD,itemB,itemA";
            
            while (true) {
                try {
                    loginClient.logIn(name, expectedKey);
                    break;
                } catch (InvalidKeyException e) {
                    expectedKey = e.expectedKey;
                }
            }

            TProtocol searchProtocol = new TMultiplexedProtocol(muxProtocol, "Search");

            Search.Client searchClient = new Search.Client(searchProtocol);

            SearchState searchState = searchClient.search(query, searchLimit);

            Fields fields = new Fields();

            fetchLoop: while(true)
            {
                FetchResult fetchResult = searchClient.fetch(searchState);

                switch (fetchResult.state)
                {
                    case PENDING:
                        Thread.sleep(100);
                    break;
                    case ITEMS:
                        Item item = fetchResult.item;

                        if (item.isSetItemA()) {
                            ItemA itemA = item.getItemA();
                            
                            if (itemA.isSetFieldA()) {
                                fields.addField("fieldA", String.valueOf(itemA.getFieldA()));
                            }
                            
                            if (itemA.isSetFieldB()) {
                                StringBuilder sb = new StringBuilder();
                                int count = 0;
                                for (short i : itemA.getFieldB()) {
                                    if (count++ > 0)
                                        sb.append(',');
                                    sb.append(i);
                                }
                                fields.addField("fieldB", sb.toString());
                            }
                            
                            if (itemA.isSetFieldC()) {
                                fields.addField("fieldC", String.valueOf(itemA.getFieldC()));
                            }

                        } else if (item.isSetItemB()) {
                            ItemB itemB = item.getItemB();
                            
                            if (itemB.isSetFieldA()) {
                                fields.addField("fieldA", itemB.getFieldA());
                            }
                            
                            if (itemB.isSetFieldB()) {
                                StringBuilder sb = new StringBuilder();
                                int count = 0;
                                TreeSet<String> set = new TreeSet<String>();
                                for (String s : itemB.getFieldB()) {
                                    set.add(s);
                                }
                                for (String s : set) {
                                    if (count++ > 0)
                                        sb.append(',');
                                    sb.append(s);
                                }
                                fields.addField("fieldB", sb.toString());
                            }
                            
                            if (itemB.isSetFieldC()) {
                                StringBuilder sb = new StringBuilder();
                                int count = 0;
                                for (String i : itemB.getFieldC()) {
                                    if (count++ > 0)
                                        sb.append(',');
                                    sb.append(i);
                                }
                                fields.addField("fieldC", sb.toString());
                            }
                        } else if (item.isSetItemC()) {
                            ItemC itemC = item.getItemC();
                            
                            if (itemC.isSetFieldA()) {
                                fields.addField("fieldA", String.valueOf(itemC.isFieldA() ? 1 : 0));
                            }
                        }
                    break;
                    case ENDED:
                        break fetchLoop;
                }

                searchState = fetchResult.nextSearchState;
            }

            TProtocol reportsProtocol = new TMultiplexedProtocol(muxProtocol, "Reports");

            Reports.Client reportsClient = new Reports.Client(reportsProtocol);

            if (!reportsClient.saveReport(fields.fields)) {
                System.out.println("Something went wrong..");
            } else {
                System.out.println("Everything went well.");
            }

            loginClient.logOut();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
