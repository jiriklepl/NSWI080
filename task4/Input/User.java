import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable {
    private String selectedDocument;
    private ArrayList<String> favorites;
    private int lastFavorite;

    public User() {
        selectedDocument = null;
        favorites = new ArrayList<String>();
        lastFavorite = 1;
    }

    public String getSelectedDocument() {
        return selectedDocument;
    }

    public void setSelectedDocument(String documentName) {
        selectedDocument = documentName;
    }

    public void addFavorite(String document) {
        favorites.add(document);
    }

    public void removeFavorite(String document) {
        favorites.remove(document);
    }

    public String nextFavorite() {
        if (++lastFavorite > favorites.size()) {
            if (favorites.size() == 0) {
                return null;
            }

            lastFavorite = 1;
        }

        return favorites.get(lastFavorite - 1);
    }

    public ArrayList<String> getFavorites() {
        return favorites;
    }

}