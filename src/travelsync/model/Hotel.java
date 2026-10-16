package travelsync.model;

public class Hotel {
    private int id;
    private String name;
    private int destinationId;
    private String description;
    private double pricePerNight;

    public Hotel(int id, String name, int destinationId, String description, double pricePerNight) {
        this.id = id;
        this.name = name;
        this.destinationId = destinationId;
        this.description = description;
        this.pricePerNight = pricePerNight;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getDestinationId() { return destinationId; }
    public String getDescription() { return description; }
    public double getPricePerNight() { return pricePerNight; }

    @Override
    public String toString() { return name; }
}