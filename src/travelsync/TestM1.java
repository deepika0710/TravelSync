package travelsync;

import travelsync.model.*;
import travelsync.strings.*;

public class TestM1 {
    public static void main(String[] args) {
        System.out.println("KMP 'beach' in Goa description: "
                + KMP.searchIgnoreCase(SampleData.destinations().get(0).getDescription(), "beach"));

        System.out.println("Z-search 'sea' in Blue Dome: "
                + ZFunction.search(SampleData.hotels().get(5).getDescription(), "sea"));

        System.out.println("Repeated phrase: "
                + ZFunction.longestRepeatedPrefix("golden beaches golden beaches"));

        System.out.println("Rabin-Karp '0617' bookings: "
                + RabinKarp.findBookings(SampleData.bookings(), "00618"));
    }
}