import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Enumeration;

import javax.jms.*;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class Client {
	
	/****	CONSTANTS	****/
	
	// name of the property specifying client's name
	public static final String CLIENT_NAME_PROPERTY = "clientName";

	// name of the topic for publishing offers
	public static final String OFFER_TOPIC = "Offers";
	private static final String SALE_QUEUE_SUFFIX = "SaleQueue";
	private static final String BUYER_NAME = "BuyerName";
	private static final String SELLER_NAME = "BuyerName";
	private static final String GOODS_NAME = "GoodsName";
	private static final String GOODS_PRICE = "GoodsPrice";
	private static final String BUYER_ACCOUNT = "BuyerAccount";
	private static final String SELLER_ACCOUNT = "SellerAccount";
	private static final String SALE_ANSWER = "SaleAnswer";
	private static final String SALE_ANSWER_DENIED = "DENIED";
	private static final String SALE_ANSWER_ACCEPT = "ACCEPT";
	private static final String SALE_CONFIRMATION = "CONFIRM";
	private static final String SALE_CANCELATION = "CANCEL";
	private static final String OFFER_ACTION = "OfferAction";
	private static final String OFFER_ACTION_OFFER = "OFFER";
	private static final String OFFER_ACTION_FETCH = "FETCH";
	
	/****	PRIVATE VARIABLES	****/
	
	// client's unique name
	private String clientName;

	// client's account number
	private int accountNumber;
	
	// offered goods, mapped by name
	private Map<String, Goods> offeredGoods = new HashMap<String, Goods>();
	
	// available goods, mapped by seller's name 
	private Map<String, List<Goods>> availableGoods = new HashMap<String, List<Goods>>();
	
	// reserved goods, mapped by name of the buyer
	private Map<String, Goods> reservedGoods = new HashMap<String, Goods>();
	
	// buyer's names, mapped by their account numbers
	private Map<Integer, String> reserverAccounts = new HashMap<Integer, String>();
	
	// buyer's reply destinations, mapped by their names
	private Map<String, Destination> reserverDestinations= new HashMap<String, Destination>();
	
	// connection to the broker
	private Connection conn;
	
	// session for user-initiated synchronous messages
	private Session clientSession;

	// session for listening and reacting to asynchronous messages
	private Session eventSession;
	
	// sender for the clientSession
	private MessageProducer clientSender;
	
	// sender for the eventSession
	private MessageProducer eventSender;

	// receiver of synchronous replies
	private MessageConsumer replyReceiver;

	// receiver of offers
	private MessageConsumer offerConsumer;

	// receiver of sales
	private MessageConsumer saleConsumer;
	
	// topic to send and receiver offers
	private Topic offerTopic;
	
	// queue for sending messages to bank
	private Queue toBankQueue;
	
	// queue for receiving synchronous replies
	private Queue replyQueue;
	private Queue saleQueue;


	
	// reader of lines from stdin
	private LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
	
	/****	PRIVATE METHODS	****/
	
	/*
	 * Constructor, stores clientName, connection and initializes maps
	 */
	private Client(String clientName, Connection conn) {
		this.clientName = clientName;
		this.conn = conn;
		
		// generate some goods
		generateGoods();
	}
	
	/*
	 * Generate goods items
	 */
	private void generateGoods() {
		Random rnd = new Random();
		for (int i = 0; i < 10; ++i) {
			String name = "";
			
			for (int j = 0; j < 4; ++j) {
				char c = (char) ('A' + rnd.nextInt('Z' - 'A'));
				name += c;
			}
			
			offeredGoods.put(name, new Goods(name, rnd.nextInt(10000)));
		}
	}
	
	/*
	 * Set up all JMS entities, get bank account, publish first goods offer 
	 */
	private void connect() throws JMSException {
		// create two sessions - one for synchronous and one for asynchronous processing
		clientSession = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		eventSession = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		
		// create (unbound) senders for the sessions
		clientSender = clientSession.createProducer(null);
		eventSender = eventSession.createProducer(null);
		
		// create queue for sending messages to bank
		toBankQueue = clientSession.createQueue(Bank.BANK_QUEUE);
		// create a temporary queue for receiving messages from bank
		Queue fromBankQueue = eventSession.createTemporaryQueue();

		// temporary receiver for the first reply from bank
		// note that although the receiver is created within a different session
		// than the queue, it is OK since the queue is used only within the
		// client session for the moment
		MessageConsumer tmpBankReceiver = clientSession.createConsumer(fromBankQueue);        
		
		// start processing messages
		conn.start();
		
		// request a bank account number
		Message msg = eventSession.createTextMessage(Bank.NEW_ACCOUNT_MSG);
		msg.setStringProperty(CLIENT_NAME_PROPERTY, clientName);
		// set ReplyTo that Bank will use to send me reply and later transfer reports
		msg.setJMSReplyTo(fromBankQueue);
		clientSender.send(toBankQueue, msg);
		
		// get reply from bank and store the account number
		TextMessage reply = (TextMessage) tmpBankReceiver.receive();
		accountNumber = Integer.parseInt(reply.getText());
		System.out.println("Account number: " + accountNumber);
		
		// close the temporary receiver
		tmpBankReceiver.close();
		
		// temporarily stop processing messages to finish initialization
		conn.stop();
		
		/* Processing bank reports */
		
		// create consumer of bank reports (from the fromBankQueue) on the event session
		MessageConsumer bankReceiver = eventSession.createConsumer(fromBankQueue);
		
		// set asynchronous listener for reports, using anonymous MessageListener
		// which just calls our designated method in its onMessage method
		bankReceiver.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message msg) {
				try {
					processBankReport(msg);
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		});

		// TO-DONE (I will use this to mark the parts I done so I can check them later if they are correct)
		
		/* Step 1: Processing offers */
		
		// create a topic both for publishing and receiving offers
		// hint: Sessions have a createTopic() method
		offerTopic = eventSession.createTopic(OFFER_TOPIC);
		
		// create a consumer of offers from the topic using the event session
		offerConsumer = eventSession.createConsumer(offerTopic);
		
		// set asynchronous listener for offers (see above how it can be done)
		// which should call processOffer()
		offerConsumer.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message msg) {
				try {
					processOffer(msg);
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		});

		/* Step 2: Processing sale requests */
		
		// create a queue for receiving sale requests (hint: Session has createQueue() method)
		// note that Session's createTemporaryQueue() is not usable here, the queue must have a name
		// that others will be able to determine from clientName (such as clientName + "SaleQueue")
		saleQueue = eventSession.createQueue(clientName + SALE_QUEUE_SUFFIX);
		    
		// create consumer of sale requests on the event session
		saleConsumer = eventSession.createConsumer(saleQueue);
		    
		// set asynchronous listener for sale requests (see above how it can be done)
		// which should call processSale()
		saleConsumer.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message msg) {
				try {
					processSale(msg);
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		});
		
		// create temporary queue for synchronous replies
		replyQueue = clientSession.createTemporaryQueue();
		
		// create synchronous receiver of the replies
		replyReceiver = clientSession.createConsumer(replyQueue);
		
		// restart message processing
		conn.start();
		
		// send list of offered goods
		fetchOffers();
		publishGoodsList(clientSender, clientSession);
	}

	/*
	 * Publish a list of offered goods
	 * Parameter is an (unbound) sender that fits into current session
	 * Sometimes we publish the list on user's request, sometimes we react to an event
	 */
	private void publishGoodsList(MessageProducer sender, Session session) throws JMSException {
		// TO-DONE (for the REGEX CHECK LATER)
		// create a message (of appropriate type) holding the list of offered goods
		// which can be created like this: new ArrayList<Goods>(offeredGoods.values())
		MapMessage offerMessage = session.createMapMessage();

		for (String name : offeredGoods.keySet()) {
			offerMessage.setInt(name, offeredGoods.get(name).price);
		}

		// don't forget to include the clientName in the message so other clients know
		// who is sending the offer - see how connect() does it when sending message to bank
		offerMessage.setStringProperty(SELLER_NAME, clientName);
		offerMessage.setStringProperty(OFFER_ACTION, OFFER_ACTION_OFFER);
		
		// send the message using the sender passed as parameter 
		sender.send(offerTopic, offerMessage);
	}
	
	/*
	 * Send empty offer and disconnect from the broker 
	 */
	private void disconnect() throws JMSException {
		// delete all offered goods
		offeredGoods.clear();
		
		// send the empty list to indicate client quit
		publishGoodsList(clientSender, clientSession);
		
		// close the connection to broker
		conn.close();
	}
	
	/*
	 * Print known goods that are offered by other clients
	 */
	private void list() {
		System.out.println("Available goods (name: price):");
		// iterate over sellers
		for (String sellerName : availableGoods.keySet()) {
			System.out.println("From " + sellerName);
			// iterate over goods offered by a seller
			for (Goods g : availableGoods.get(sellerName)) {
				System.out.println("  " + g);
			}
		}
	}
	
	/*
	 * Main interactive user loop
	 */
	private void loop() throws IOException, JMSException {
		// first connect to broker and setup everything
		connect();
		
		loop:
		while (true) {
			System.out.println("\nAvailable commands (type and press enter):");
			System.out.println(" l - list available goods");
			System.out.println(" p - publish list of offered goods");
			System.out.println(" f - fetch lists of offered goods");
			System.out.println(" b - buy goods");
			System.out.println(" c - check balance");
			System.out.println(" q - quit");
			// read first character
			int c = in.read();
			// throw away rest of the buffered line
			while (in.ready()) in.read();
			switch (c) {
				case 'q':
					disconnect();
					break loop;
				case 'b':
					buy();
					break;
				case 'l':
					list();
					break;
				case 'c':
					check();
					break;
				case 'f':
					fetchOffers();
					break;
				case 'p':
					publishGoodsList(clientSender, clientSession);
					System.out.println("List of offers published");
					break;
				case '\n':
				default:
					break;
			}
		}
	}

	void check() throws JMSException {
		// request a bank account number
		Message msg = eventSession.createTextMessage(Bank.GET_BALANCE);
		msg.setStringProperty(CLIENT_NAME_PROPERTY, clientName);
		// set ReplyTo that Bank will use to send me reply and later transfer reports
		msg.setJMSReplyTo(replyQueue);
		clientSender.send(toBankQueue, msg);
		
		// get reply from bank and store the account number
		TextMessage reply = (TextMessage) replyReceiver.receive();
		String result = reply.getText();
		if (result != null) {
			int accountBalance = Integer.parseInt(result);
			System.out.println("Account number: " + accountNumber);
			System.out.println("Account balance: " + accountBalance);
		} else {
			System.out.println("Invalid request.");
		}
	}
	
	/*
	 * Perform buying of goods
	 */
	private void buy() throws IOException, JMSException {
		// get information from the user
		System.out.println("Enter seller name:");
		String sellerName = in.readLine();
		System.out.println("Enter goods name:");
		String goodsName = in.readLine();

		// check if the seller exists
		List<Goods> sellerGoods = availableGoods.get(sellerName);
		if (sellerGoods == null) {
			System.out.println("Seller does not exist: " + sellerName);
			return;
		}
		
		// TO-DONE (for the REGEX CHECK LATER)
		
		// First consider what message types clients will use for communicating a sale
		// we will need to transfer multiple values (of String and int) in each message 
		// MapMessage? ObjectMessage? TextMessage with extra properties?
		
		/* Step 1: send a message to the seller requesting the goods */
		
		// create local reference to the seller's queue
		// similar to Step 2 in connect() but using sellerName instead of clientName
		Queue saleQueue = clientSession.createQueue(sellerName + SALE_QUEUE_SUFFIX);
		
		// create message requesting sale of the goods
		MapMessage goodsRequest = clientSession.createMapMessage();
		
		// includes: clientName, goodsName, accountNumber
		goodsRequest.setInt(BUYER_ACCOUNT, accountNumber);
		goodsRequest.setString(BUYER_NAME, clientName);
		goodsRequest.setString(GOODS_NAME, goodsName);
		
		// also include reply destination that the other client will use to send reply (replyQueue)
		// how? see how connect() uses SetJMSReplyTo() 
		goodsRequest.setJMSReplyTo(replyQueue);
					
		// send the message (with clientSender)
		clientSender.send(saleQueue, goodsRequest);
		
		/* Step 2: get seller's response and process it */
		
		// receive the reply (synchronously, using replyReceiver)
		MapMessage replyMessage = (MapMessage)replyReceiver.receive();
		
		// parse the reply (depends on your selected message format)
		// distinguish between "sell denied" and "sell accepted" message
		// in case of "denied", report to user and return from this method
		if (replyMessage.getString(SALE_ANSWER).equals(SALE_ANSWER_DENIED)) {
			System.out.println("Your request for denied.");
			return;
		}
		// in case of "accepted"
		// - obtain seller's account number and price to pay
		int price = replyMessage.getInt(GOODS_PRICE);
		int sellerAccount = replyMessage.getInt(SELLER_ACCOUNT);

		/* Step 3: send message to bank requesting money transfer */
		
		// create message ordering the bank to send money to seller
		MapMessage bankMsg = clientSession.createMapMessage();
		bankMsg.setStringProperty(CLIENT_NAME_PROPERTY, clientName);
		bankMsg.setInt(Bank.ORDER_TYPE_KEY, Bank.ORDER_TYPE_SEND);
		bankMsg.setInt(Bank.ORDER_RECEIVER_ACC_KEY, sellerAccount);
		bankMsg.setInt(Bank.AMOUNT_KEY, price);
		
		System.out.println("Sending $" + price + " to account " + sellerAccount);
		
		// send message to the bank
		clientSender.send(toBankQueue, bankMsg);

		/* Step 4: wait for seller's sale confirmation */
		
		// receive the confirmation, similar to Step 2
		TextMessage confirmMessage = (TextMessage)replyReceiver.receive();
		String confirmedName = confirmMessage.getStringProperty(GOODS_NAME);
		
		// parse message and verify it's confirmation message
		String answer = confirmMessage.getText();
		if (answer.equals(SALE_CONFIRMATION) && confirmedName.equals(goodsName)) {
			// report successful sale to the user
			System.out.println("Your request was confirmed!");
		}
		else if (answer.equals(SALE_CANCELATION)) {
			System.out.println("Your request was canceled.");
		} else {
			System.out.println("Encountered unknown error.");
		}
		
	}

	private void fetchOffers() throws JMSException {
		MapMessage offerMessage = clientSession.createMapMessage();

		for (String name : offeredGoods.keySet()) {
			offerMessage.setInt(name, offeredGoods.get(name).price);
		}

		// don't forget to include the clientName in the message so other clients know
		// who is sending the offer - see how connect() does it when sending message to bank
		offerMessage.setStringProperty(SELLER_NAME, clientName);
		offerMessage.setStringProperty(OFFER_ACTION, OFFER_ACTION_FETCH);
		
		// send the message using the sender passed as parameter 
		clientSender.send(offerTopic, offerMessage);
	}
	
	/*
	 * Process a message with goods offer
	 */
	private void processOffer(Message msg) throws JMSException {
		// TO-DONE (for the REGEX CHECK LATER)
		
		// parse the message, obtaining sender's name and list of offered goods
		MapMessage offerMessage = (MapMessage)msg;
		String senderName = offerMessage.getStringProperty(SELLER_NAME);
		@SuppressWarnings("unchecked")
		Enumeration<String> goodsNames = offerMessage.getMapNames();
		
		// should ignore messages sent from myself
		if (clientName.equals(senderName))
			return;

		String action = offerMessage.getStringProperty(OFFER_ACTION);
		if (action.equals(OFFER_ACTION_FETCH)) {
			publishGoodsList(eventSender, eventSession);
		}

		if (!action.equals(OFFER_ACTION_OFFER)) {
			return; // we don' recognize this request/action
		}


		// store the list into availableGoods (replacing any previous offer)
		// empty list means disconnecting client, remove it from availableGoods completely
		if (!goodsNames.hasMoreElements()) {
			availableGoods.remove(senderName);
		} else {
			List<Goods> sellerGoods = new ArrayList<Goods>();
			
			do {
				String name = goodsNames.nextElement();
				sellerGoods.add(new Goods(name, offerMessage.getInt(name)));

			} while (goodsNames.hasMoreElements());

			availableGoods.put(senderName, sellerGoods);
		}
	}
	
	/*
	 * Process message requesting a sale
	 */
	private void processSale(Message msg) throws JMSException {
		// TO-DONE (for the REGEX CHECK LATER)
		
		/* Step 1: parse the message */
		
		
		// distinguish that it's the sale request message
		MapMessage goodsRequest = (MapMessage)msg;

		// obtain buyer's name (buyerName), goods name (goodsName) , buyer's account number (buyerAccount)
		String buyerName = goodsRequest.getString(BUYER_NAME);
		String goodsName = goodsRequest.getString(GOODS_NAME);
		int buyerAccount = goodsRequest.getInt(BUYER_ACCOUNT);

		
		// also obtain reply destination (buyerDest)
		Queue buyerDest = (Queue)goodsRequest.getJMSReplyTo();

		// how? see for example Bank.processTextMessage()

		/* Step 2: decide what to do and modify data structures accordingly */
		/* Step 3: send reply message */

		// prepare reply message (accept or deny)
		MapMessage replyMessage = eventSession.createMapMessage();

		// the client is free to send us their money in every case!
		replyMessage.setInt(SELLER_ACCOUNT, accountNumber); 
		
		// check if we still offer this goods
		Goods goods = offeredGoods.remove(goodsName);
		if (goods != null) {
			// if yes, we should remove it from offeredGoods and publish new list
			// also it's useful to create a list of "reserved goods" together with buyer's information
			// such as name, account number, reply destination
			reservedGoods.put(buyerName, goods);
			reserverAccounts.put(buyerAccount, buyerName);
			reserverDestinations.put(buyerName, buyerDest);

			// accept message includes: my account number (accountNumber), price (goods.price)
			replyMessage.setInt(GOODS_PRICE, goods.price);
			replyMessage.setString(SALE_ANSWER, SALE_ANSWER_ACCEPT);
		} else {
			replyMessage.setString(SALE_ANSWER, SALE_ANSWER_DENIED);
		}

		// send reply
		eventSender.send(buyerDest, replyMessage);
	}
	
	/*
	 * Process message with (transfer) report from the bank
	 */
	private void processBankReport(Message msg) throws JMSException {
		/* Step 1: parse the message */
		
		// Bank reports are sent as MapMessage
		if (msg instanceof MapMessage) {
			MapMessage mapMsg = (MapMessage) msg;
			// get report number
			int cmd = mapMsg.getInt(Bank.REPORT_TYPE_KEY);

			// get account number of sender and the amount of money sent
			int buyerAccount = mapMsg.getInt(Bank.REPORT_SENDER_ACC_KEY);
			int amount = mapMsg.getInt(Bank.AMOUNT_KEY);
			// match the sender account with sender
			String buyerName = reserverAccounts.get(buyerAccount);
			Destination buyerDest = reserverDestinations.get(buyerName);

			Goods g = reservedGoods.remove(buyerName);
			reserverDestinations.remove(buyerName);
			reserverAccounts.remove(buyerAccount);

			if (cmd == Bank.REPORT_TYPE_RECEIVED) {
				
				
				// match the reserved goods
				
				System.out.println("Received $" + amount + " from " + buyerName);
				
				/* Step 2: decide what to do and modify data structures accordingly */
				
				// did he pay enough?
				if (g != null && amount >= g.price) {
					// get the buyer's destination

					// remove the reserved goods and buyer-related information
					
					// TO-DONE (for the REGEX CHECK LATER)
					/* Step 3: send confirmation message */
					
					// prepare sale confirmation message
					TextMessage reply = eventSession.createTextMessage(SALE_CONFIRMATION);
					// includes: goods name (g.name)
					reply.setStringProperty(GOODS_NAME, g.name);
					
					// send reply (destination is buyerDest)
					eventSender.send(buyerDest, reply);
				} else {
					// The client tried to rob us, we keep the money (if there is any)
					if (g != null)
						offeredGoods.put(g.name, g);
					TextMessage reply = this.clientSession.createTextMessage(SALE_CANCELATION);
					eventSender.send(buyerDest, reply);
				}
			} else if (cmd == Bank.REPORT_TYPE_CANCELED) {
				if (g != null)
					offeredGoods.put(g.name, g);
				TextMessage reply = this.clientSession.createTextMessage(SALE_CANCELATION);
				eventSender.send(buyerDest, reply);

			} else {
				System.out.println("Received unknown MapMessage:\n: " + msg);
			}
		} else {
			System.out.println("Received unknown message:\n: " + msg);
		}
	}
	
	/**** PUBLIC METHODS ****/
	
	/*
	 * Main method, creates client instance and runs its loop
	 */
	public static void main(String[] args) {

		if (args.length != 1) {
			System.err.println("Usage: ./client <clientName>");
			return;
		}
		
		// create connection to the broker.
		try (ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
				Connection connection = connectionFactory.createConnection()) {
			// create instance of the client
			Client client = new Client(args[0], connection);
			
			// perform client loop
			client.loop();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
