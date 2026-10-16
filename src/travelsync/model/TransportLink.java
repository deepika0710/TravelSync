package travelsync.model;

public class TransportLink {
    private int from;      // node number of start city
    private int to;        // node number of end city
    private int capacity;  // max passengers per day

    public TransportLink(int from, int to, int capacity) {
        this.from = from;
        this.to = to;
        this.capacity = capacity;
    }

    public int getFrom() { return from; }
    public int getTo() { return to; }
    public int getCapacity() { return capacity; }
}