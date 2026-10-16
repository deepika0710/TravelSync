package travelsync.model;

public class Destination {
    private int id;
    private String name;
    private String country;
    private String description;
    private int searchCount; // how often people search it

    public Destination(int id, String name, String country, String description, int searchCount) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.description = description;
        this.searchCount = searchCount;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getDescription() { return description; }
    public int getSearchCount() { return searchCount; }

    @Override
    public String toString() { return name + " (" + country + ")"; }
}