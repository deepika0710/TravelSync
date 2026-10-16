package travelsync.app;

// One booking made by one user. This is what gets saved in data/bookings.txt
public class Reservation {
    public final String code;      // e.g. BK-2026-00852
    public final String username;  // who booked
    public final int hotelId;
    public final int nights;
    public final long createdAt;   // time of booking, in milliseconds

    public Reservation(String code, String username, int hotelId, int nights, long createdAt) {
        this.code = code;
        this.username = username;
        this.hotelId = hotelId;
        this.nights = nights;
        this.createdAt = createdAt;
    }
}