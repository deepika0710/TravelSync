package travelsync.model;

public class Booking {
    private String code; // e.g. "BK-2026-00417"
    private String customerName;
    private int hotelId;
    private int nights;

    public Booking(String code, String customerName, int hotelId, int nights) {
        this.code = code;
        this.customerName = customerName;
        this.hotelId = hotelId;
        this.nights = nights;
    }

    public String getCode() { return code; }
    public String getCustomerName() { return customerName; }
    public int getHotelId() { return hotelId; }
    public int getNights() { return nights; }

    @Override
    public String toString() { return code + " - " + customerName; }
}