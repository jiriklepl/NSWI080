import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import java.util.ArrayList;

public class Client {
	// Reader for user input
	private LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
	// Connection to the cluster
	private HazelcastInstance hazelcast;
	// The name of the user
	private String userName;
	// Do not keep any other state here - all data should be in the cluster

	/**
	 * Create a client for the specified user.
	 * @param userName user name used to identify the user
	 */
	public Client(String userName) {
		this.userName = userName;
		// Connect to the Hazelcast cluster
		ClientConfig config = new ClientConfig();
		hazelcast = HazelcastClient.newHazelcastClient(config);
	}

	/**
	 * Disconnect from the Hazelcast cluster.
	 */
	public void disconnect() {
		// Disconnect from the Hazelcast cluster
		hazelcast.shutdown();
	}

	private DocumentData loadDocument(String documentName) {
		IMap<String, DocumentData> documents = hazelcast.getMap("Documents");

		return documents.executeOnKey(documentName, (entry) -> {
			DocumentData documentData = entry.getValue();
			
			// Get the document (from the cache, or generated)
			if (documentData == null) {
				documentData = new DocumentData(documentName, DocumentGenerator.generateDocument(documentName));
			}
			// Increment the view count
			documentData.incrementViews();
			entry.setValue(documentData);

			return documentData;
		});
	}

	private void setSelected(String userName, DocumentData document) {
		IMap<String, User> users = hazelcast.getMap("Users");
		users.executeOnKey(userName, (entry) -> {
			User user = entry.getValue();

			if (user == null) {
				user = new User();
			}
			
			// Set the current selected document for the user
			user.setSelectedDocument(document);
			entry.setValue(user);

			return null;
		});
	}

	/**
	 * Read a name of a document,
	 * select it as the current document of the user
	 * and show the document content.
	 */
	private void showCommand() throws IOException {
		System.out.println("Enter document name:");
		String documentName = in.readLine();

		// Currently, the document is generated directly on the cluster

		DocumentData document = loadDocument(documentName);
		setSelected(userName, document);

		// Show the document content
		System.out.println("The document is:");
		System.out.println(document.getContent());
	}

	/**
	 * Show the next document in the list of favorites of the user.
	 * Select the next document, so that running this command repeatedly
	 * will cyclically show all favorite documents of the user.
	 */
	private void nextFavoriteCommand() {
		IMap<String, User> users = hazelcast.getMap("Users");

		String documentName = users.executeOnKey(userName, (entry) -> {
			User user = entry.getValue();

			if (user == null) {
				entry.setValue(user = new User());
			}

			// Select the next document form the list of favorites
			String next = user.nextFavorite();
			entry.setValue(user);

			return next;
		});

		if (documentName == null) {
			System.out.println("The favorites list is empty");
			return;
		}

		// Increment the view count, get the document (from the cache, or generated) and show the document content
		DocumentData document = loadDocument(documentName);
		setSelected(userName, document);

		System.out.println("The document is:");
		System.out.println(document.getContent());
	}

	/**
	 * Add the current selected document name to the list of favorite documents of the user.
	 * If the list already contains the document name, do nothing.
	 */
	private void addFavoriteCommand() {
		IMap<String, User> users = hazelcast.getMap("Users");
		String result = users.executeOnKey(userName, (entry) -> {
			User user = entry.getValue();
			
			if (user == null) {
				user = new User();
			}
			
			// Select the next document form the list of favorites
			DocumentData document = user.getSelectedDocument();
			
			if (document == null) {
				return "No document selected";
			}
			
			// Add the name of the selected document to the list of favorites

			String documentName = document.getName();

			user.addFavorite(documentName);
			entry.setValue(user);

			return String.format("Added %s to favorites%n", documentName);
		});
		
		System.out.printf("%s", result);
	}
	/**
	 * Remove the current selected document name from the list of favorite documents of the user.
	 * If the list does not contain the document name, do nothing.
	 */
	private void removeFavoriteCommand() {
		IMap<String, User> users = hazelcast.getMap("Users");
		String result = users.executeOnKey(userName, (entry) -> {
			User user = entry.getValue();

			if (user == null) {
				user = new User();
			}

			// Select the next document form the list of favorites
			DocumentData document = user.getSelectedDocument();

			if (document == null) {
				return "No document selected";
			}

			// Remove the name of the selected document from the list of favorites

			String documentName = document.getName();

			user.removeFavorite(documentName);
			entry.setValue(user);

			return String.format("Removed %s from favorites%n", documentName);
		});
		
		System.out.printf("%s", result);
	}
	/**
	 * Add the current selected document name to the list of favorite documents of the user.
	 * If the list already contains the document name, do nothing.
	 */
	private void listFavoritesCommand() {
		// Get the list of favorite documents of the user
		IMap<String, User> users = hazelcast.getMap("Users");
		ArrayList<String> favoriteList = users.executeOnKey(userName, (entry) -> {
			User user = entry.getValue();
			
			if (user == null) {
				entry.setValue(user = new User());
			}
			
			return user.getFavorites();
		});
		
		// Print the list of favorite documents
		System.out.println("Your list of favorite documents:");
		for (String favoriteDocumentName : favoriteList) {
			System.out.println(favoriteDocumentName);
		}
	}

	/**
	 * Show the view count and comments of the current selected document.
	 */
	private void infoCommand(){
		IMap<String, User> users = hazelcast.getMap("Users");
		DocumentData document = users.executeOnKey(userName, (entry) -> {
			User user = entry.getValue();
			
			if (user == null) {
				entry.setValue(user = new User());
			}
			
			return user.getSelectedDocument();
		});
		
		if (document == null) {
			System.out.println("No document selected");
			return;
		}
		
		// Get the view count and list of comments of the selected document
		String selectedDocumentName = document.getName();
		int viewCount = document.getViews();
		ArrayList<String> comments = document.getComments();

		// Print the information
		System.out.printf("Info about %s:%n", selectedDocumentName);
		System.out.printf("Viewed %d times.%n", viewCount);
		System.out.printf("Comments (%d):%n", comments.size());

		for (String comment: comments) {
			System.out.println(comment);
		}
	}
	/**
	 * Add a comment about the current selected document.
	 */
	private void commentCommand() throws IOException {
		System.out.println("Enter comment text:");
		String commentText = in.readLine();

		IMap<String, User> users = hazelcast.getMap("Users");
		DocumentData document = users.executeOnKey(userName, (entry) -> {
			User user = entry.getValue();
			
			if (user == null) {
				entry.setValue(user = new User());
			}
			
			return user.getSelectedDocument();
		});

		if (document == null) {
			System.out.println("No document selected");
			return;
		}

		// Add the comment to the list of comments of the selected document
		IMap<String, DocumentData> documents = hazelcast.getMap("Documents");
		String selectedDocumentName = document.getName();

		documents.executeOnKey(selectedDocumentName, (entry) -> {
			DocumentData documentData = entry.getValue();
			documentData.addComment(commentText);
			entry.setValue(documentData);

			return null;
		});


		System.out.printf("Added a comment about %s.%n", selectedDocumentName);
	}

	/*
	 * Main interactive user loop
	 */
	public void run() throws IOException {
		loop:
		while (true) {
			System.out.println("\nAvailable commands (type and press enter):");
			System.out.println(" s - select and show document");
			System.out.println(" i - show document view count and comments");
			System.out.println(" c - add comment");
			System.out.println(" a - add to favorites");
			System.out.println(" r - remove from favorites");
			System.out.println(" n - show next favorite");
			System.out.println(" l - list all favorites");
			System.out.println(" q - quit");
			// read first character
			int c = in.read();
			// throw away rest of the buffered line
			while (in.ready())
				in.read();
			switch (c) {
				case 'q': // Quit the application
					break loop;
				case 's': // Select and show a document
					showCommand();
					break;
				case 'i': // Show view count and comments of the selected document
					infoCommand();
					break;
				case 'c': // Add a comment to the selected document
					commentCommand();
					break;
				case 'a': // Add the selected document to favorites
					addFavoriteCommand();
					break;
				case 'r': // Remove the selected document from favorites
					removeFavoriteCommand();
					break;
				case 'n': // Select and show the next document in the list of favorites
					nextFavoriteCommand();
					break;
				case 'l': // Show the list of favorite documents
					listFavoritesCommand();
					break;
				case '\n':
				default:
					break;
			}
		}
	}

	/*
	 * Main method, creates a client instance and runs its loop
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: ./client <userName>");
			return;
		}

		try {
			Client client = new Client(args[0]);
			try {
				client.run();
			}
			finally {
				client.disconnect();
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
}
