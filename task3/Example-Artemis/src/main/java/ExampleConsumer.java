import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;


public class ExampleConsumer implements MessageListener {
	
	public static void main(String[] args) {
		
		// Create connection to the broker.
		// Note that the factory is usually obtained from JNDI, this method is ActiveMQ-specific
		// used here for simplicity
		try(ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://lab.d3s.mff.cuni.cz:5000", "labUser", "sieb5w9");
			Connection connection = connectionFactory.createConnection()){
			
			// Create a non-transacted, auto-acknowledged session
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			
			// Create a queue, name must match the queue created by producer
			// Note that this is also provider-specific and should be obtained from JNDI
			Topic topic = session.createTopic("LabTopic");
			MessageConsumer consumer = session.createConsumer(topic);
			consumer.setMessageListener(new ExampleConsumer());
			
			Queue queue = session.createQueue("LabQueue");
			MessageProducer producer = session.createProducer(queue);

			connection.start();
			
			// Create and set an asynchronous message listener
			Message message = session.createTextMessage("Jirka's unique nickname");
			producer.send(message);

			// Receive a message synchronously
			Thread.sleep(10000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Asynchronously receive messages
	@Override
	public void onMessage(Message msg) {
		// Print the message
		try {
			if (msg instanceof TextMessage) {
				TextMessage txt = (TextMessage) msg;
				System.out.println("Asynchronous: " + txt.getText());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
