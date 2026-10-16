package travelsync.model;

import java.util.ArrayList;
import java.util.List;

public class SampleData {

    public static List<Destination> destinations() {
        List<Destination> list = new ArrayList<>();
        list.add(new Destination(1, "Goa", "India", "Golden beaches and lively nightlife. Golden beaches and fresh seafood.", 950));
        list.add(new Destination(2, "Paris", "France", "City of lights with art museums, cafes and the Eiffel Tower.", 1200));
        list.add(new Destination(3, "Bali", "Indonesia", "Tropical island with beaches, temples and rice terraces.", 800));
        list.add(new Destination(4, "Kyoto", "Japan", "Ancient temples, gardens and traditional tea houses.", 600));
        list.add(new Destination(5, "Manali", "India", "Snowy mountains, adventure sports and cozy cafes.", 700));
        list.add(new Destination(6, "Santorini", "Greece", "White buildings, blue domes and sunset views over the sea.", 500));
        return list;
    }

    public static List<Hotel> hotels() {
        List<Hotel> list = new ArrayList<>();
        list.add(new Hotel(1, "Sea Breeze Resort", 1, "Beachfront rooms with sea view and pool.", 90));
        list.add(new Hotel(2, "Grand Paris Hotel", 2, "Luxury rooms near the Eiffel Tower.", 250));
        list.add(new Hotel(3, "Ubud Jungle Villa", 3, "Private villas surrounded by rice terraces.", 120));
        list.add(new Hotel(4, "Kyoto Garden Inn", 4, "Traditional inn with a peaceful garden.", 140));
        list.add(new Hotel(5, "Snow Peak Lodge", 5, "Warm wooden cabins with mountain views.", 70));
        list.add(new Hotel(6, "Blue Dome Suites", 6, "Cliffside suites with sunset sea views.", 300));
        return list;
    }

    public static List<Booking> bookings() {
        List<Booking> list = new ArrayList<>();
        list.add(new Booking("BK-2026-00417", "Anita", 1, 3));
        list.add(new Booking("BK-2026-00532", "Ravi", 2, 5));
        list.add(new Booking("BK-2026-00618", "Meera", 3, 4));
        list.add(new Booking("BK-2026-00744", "John", 4, 2));
        list.add(new Booking("BK-2026-00851", "Sara", 5, 6));
        return list;
    }
}