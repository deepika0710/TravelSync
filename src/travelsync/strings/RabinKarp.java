package travelsync.strings;

import java.util.ArrayList;
import java.util.List;
import travelsync.model.Booking;

public class RabinKarp {

    private static final long BASE = 256;
    private static final long MOD = 1_000_000_007L;

    public static List<Integer> search(String text, String pattern) {
        List<Integer> result = new ArrayList<>();
        int m = pattern.length();
        int n = text.length();
        if (m == 0 || m > n) return result;

        long h = 1; // BASE^(m-1) % MOD
        for (int i = 0; i < m - 1; i++) {
            h = (h * BASE) % MOD;
        }

        long patternHash = 0;
        long textHash = 0;
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * BASE + pattern.charAt(i)) % MOD;
            textHash = (textHash * BASE + text.charAt(i)) % MOD;
        }

        for (int i = 0; i <= n - m; i++) {
            // same fingerprint -> double check the real letters
            if (patternHash == textHash && text.regionMatches(i, pattern, 0, m)) {
                result.add(i);
            }
            if (i < n - m) {
                textHash = (textHash - text.charAt(i) * h % MOD + MOD) % MOD;
                textHash = (textHash * BASE + text.charAt(i + m)) % MOD;
            }
        }
        return result;
    }

    // find all bookings whose code contains the query
    public static List<Booking> findBookings(List<Booking> bookings, String query) {
        List<Booking> found = new ArrayList<>();
        for (Booking b : bookings) {
            if (!search(b.getCode(), query).isEmpty()) {
                found.add(b);
            }
        }
        return found;
    }
}