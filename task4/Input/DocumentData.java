import java.io.Serializable;
import java.util.ArrayList;

/**
 * Represents some kind of document that can be displayed to the user.
 */
public class DocumentData implements Serializable {
	private String name;
	private Document document;
	private int views;
	private ArrayList<String> comments;

	public DocumentData(String name, Document document) {
		this.name = name;
	    this.document = document;
		this.views = 0;
		this.comments = new ArrayList<String>();
	}

	public void incrementViews() {
		views++;
	}

	public int getViews() {
		return views;
	}

	public void addComment(String comment) {
		comments.add(comment);
	}

	public ArrayList<String> getComments() {
		return comments;
	}

	public String getName() {
	    return name;
	}

	public String getContent() {
	    return document.getContent();
	}
}