package travelsync.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import travelsync.model.Booking;

// Keeps users and bookings in memory AND saves them in two text files,
// so nothing is lost when the server stops.
//   data/users.txt     ->  username|salt|passwordHash
//   data/bookings.txt  ->  code|username|hotelId|nights|time
public class DataStore {

    private static final Path DATA_DIR = Path.of("data");
    private static final Path USERS_FILE = DATA_DIR.resolve("users.txt");
    private static final Path BOOKINGS_FILE = DATA_DIR.resolve("bookings.txt");

    private final Map<String, String[]> users = new HashMap<>();    // username -> {salt, hash}
    private final List<Reservation> reservations = new ArrayList<>();
    private final Map<String, String> sessions = new HashMap<>();   // login token -> username
    private final SecureRandom random = new SecureRandom();
    private int nextNumber = 1;

    public DataStore() {
        load();
    }

    // ---------- loading and saving ----------
    private void load() {
        try {
            Files.createDirectories(DATA_DIR);

            if (Files.exists(USERS_FILE)) {
                for (String line : Files.readAllLines(USERS_FILE, StandardCharsets.UTF_8)) {
                    String[] p = line.split("\\|");
                    if (p.length == 3) users.put(p[0], new String[] { p[1], p[2] });
                }
            }

            if (Files.exists(BOOKINGS_FILE)) {
                for (String line : Files.readAllLines(BOOKINGS_FILE, StandardCharsets.UTF_8)) {
                    String[] p = line.split("\\|");
                    if (p.length == 5) {
                        reservations.add(new Reservation(p[0], p[1],
                                Integer.parseInt(p[2]), Integer.parseInt(p[3]), Long.parseLong(p[4])));
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Could not read the data files: " + e.getMessage());
        }
        for (Reservation r : reservations) {
            nextNumber = Math.max(nextNumber, numberOf(r.code) + 1);
        }
    }

    // First run only: start with the sample bookings so the live feed is not empty
    public synchronized void seedIfEmpty(List<Booking> samples) {
        if (!reservations.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (int i = 0; i < samples.size(); i++) {
            Booking b = samples.get(i);
            Reservation r = new Reservation(b.getCode(), b.getCustomerName(),
                    b.getHotelId(), b.getNights(), now - (long) (samples.size() - i) * 7 * 60_000);
            reservations.add(r);
            saveLine(BOOKINGS_FILE, toLine(r));
            nextNumber = Math.max(nextNumber, numberOf(r.code) + 1);
        }
    }

    private static String toLine(Reservation r) {
        return r.code + "|" + r.username + "|" + r.hotelId + "|" + r.nights + "|" + r.createdAt;
    }

    private static void saveLine(Path file, String line) {
        try {
            Files.writeString(file, line + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Could not save to " + file + ": " + e.getMessage());
        }
    }

    private static int numberOf(String code) {
        try {
            return Integer.parseInt(code.substring(code.lastIndexOf('-') + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ---------- accounts ----------
    // Returns an error message, or null if the account was created.
    public synchronized String signUp(String username, String password) {
        if (!username.matches("[A-Za-z0-9_]{3,20}")) {
            return "Username must be 3 to 20 letters, numbers or underscores.";
        }
        if (password.length() < 4) {
            return "Password must be at least 4 characters.";
        }
        if (canonical(username) != null) {
            return "That username is already taken.";
        }
        String salt = randomHex(8);
        String hash = hash(salt, password);
        users.put(username, new String[] { salt, hash });
        saveLine(USERS_FILE, username + "|" + salt + "|" + hash);
        return null;
    }

    // Returns a login token, or null if the username or password is wrong.
    public synchronized String login(String username, String password) {
        String name = canonical(username);
        if (name == null) return null;
        String[] saved = users.get(name);
        if (!hash(saved[0], password).equals(saved[1])) return null;
        String token = randomHex(16);
        sessions.put(token, name);
        return token;
    }

    public synchronized String usernameForToken(String token) {
        return sessions.get(token);
    }

    public synchronized void logout(String token) {
        sessions.remove(token);
    }

    // finds the stored spelling of a username, ignoring upper/lower case
    private String canonical(String username) {
        for (String u : users.keySet()) {
            if (u.equalsIgnoreCase(username)) return u;
        }
        return null;
    }

    // We never store the password itself, only a scrambled version (salt + SHA-256)
    private static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String randomHex(int numBytes) {
        byte[] bytes = new byte[numBytes];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ---------- bookings ----------
    public synchronized Reservation addReservation(String username, int hotelId, int nights) {
        String code = String.format(Locale.US, "BK-2026-%05d", nextNumber++);
        Reservation r = new Reservation(code, username, hotelId, nights, System.currentTimeMillis());
        reservations.add(r);
        saveLine(BOOKINGS_FILE, toLine(r));
        return r;
    }

    public synchronized List<Reservation> all() {
        return new ArrayList<>(reservations);
    }

    public synchronized List<Reservation> forUser(String username) {
        List<Reservation> mine = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.username.equals(username)) mine.add(r);
        }
        return mine;
    }

    // the same bookings in the older Booking form (used by the Rabin-Karp code search)
    public synchronized List<Booking> asBookings() {
        List<Booking> list = new ArrayList<>();
        for (Reservation r : reservations) {
            list.add(new Booking(r.code, r.username, r.hotelId, r.nights));
        }
        return list;
    }
}