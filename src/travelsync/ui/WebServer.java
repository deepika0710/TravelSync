package travelsync.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import travelsync.app.*;
import travelsync.dp.*;
import travelsync.flow.*;
import travelsync.model.*;
import travelsync.np.*;
import travelsync.random.*;
import travelsync.strings.*;
import travelsync.suffix.*;

public class WebServer {

    private static final List<Destination> destinations = SampleData.destinations();
    private static final List<Hotel> hotels = SampleData.hotels();
    private static final TravelIndex travelIndex = new TravelIndex(destinations, hotels);
    private static final DataStore store = new DataStore();   // users + bookings (saved in the data folder)

    public static void main(String[] args) throws IOException {
        store.seedIfEmpty(SampleData.bookings());

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        // ----- the traveler app: accounts, search, booking, live feed -----
        server.createContext("/api/signup", WebServer::signup);
        server.createContext("/api/login", WebServer::login);
        server.createContext("/api/logout", WebServer::logout);
        server.createContext("/api/me", WebServer::me);
        server.createContext("/api/destinations", WebServer::destinationsApi);
        server.createContext("/api/book", WebServer::book);
        server.createContext("/api/mytrips", WebServer::myTrips);
        server.createContext("/api/feed", WebServer::feed);
        server.createContext("/api/simulate", WebServer::simulate);

        // ----- M1 -----
        server.createContext("/api/kmp-search", WebServer::kmpSearch);
        server.createContext("/api/rabinkarp", WebServer::rabinKarpSearch);

        // ----- M2 -----
        server.createContext("/api/suffix-search", WebServer::suffixSearch);
        server.createContext("/api/similar", WebServer::similar);

        // ----- M3 -----
        server.createContext("/api/spellcheck", WebServer::spellCheck);
        server.createContext("/api/trip", WebServer::trip);

        // ----- M4 -----
        server.createContext("/api/flow", WebServer::flow);
        server.createContext("/api/mincut", WebServer::minCut);
        server.createContext("/api/match", WebServer::match);

        // ----- M5 -----
        server.createContext("/api/np", WebServer::np);
        server.createContext("/api/vertexcover", WebServer::vertexCover);

        // ----- M6 -----
        server.createContext("/api/rank", WebServer::rank);
        server.createContext("/api/reservoir", WebServer::reservoir);
        server.createContext("/api/prime", WebServer::prime);
        server.createContext("/api/scan", WebServer::scan);
        server.createContext("/api/reduce", WebServer::reduce);
        server.createContext("/api/brent", WebServer::brent);

        // ----- static frontend files -----
        server.createContext("/", WebServer::staticFile);

        server.setExecutor(null);
        server.start();
        System.out.println("TravelSync server running: http://localhost:8000");
    }

    // =====================================================================
    //  TRAVELER APP
    // =====================================================================

    // ---------- accounts ----------
    private static void signup(HttpExchange ex) throws IOException {
        if (!requirePost(ex)) return;
        Map<String, String> f = form(ex);
        String username = f.getOrDefault("username", "").trim();
        String password = f.getOrDefault("password", "");
        String error = store.signUp(username, password);
        if (error != null) {
            sendError(ex, 400, error);
            return;
        }
        sendLoginResult(ex, store.login(username, password));
    }

    private static void login(HttpExchange ex) throws IOException {
        if (!requirePost(ex)) return;
        Map<String, String> f = form(ex);
        String token = store.login(f.getOrDefault("username", "").trim(), f.getOrDefault("password", ""));
        if (token == null) {
            sendError(ex, 401, "Wrong username or password.");
            return;
        }
        sendLoginResult(ex, token);
    }

    private static void sendLoginResult(HttpExchange ex, String token) throws IOException {
        sendJson(ex, "{\"token\":" + JsonUtil.quote(token)
                + ",\"username\":" + JsonUtil.quote(store.usernameForToken(token)) + "}");
    }

    private static void logout(HttpExchange ex) throws IOException {
        String token = ex.getRequestHeaders().getFirst("X-Token");
        if (token != null) store.logout(token);
        sendJson(ex, "{\"ok\":true}");
    }

    // the browser asks this on page load: "is my saved login still valid?"
    private static void me(HttpExchange ex) throws IOException {
        String user = currentUser(ex);
        if (user == null) {
            sendError(ex, 401, "Please log in.");
            return;
        }
        sendJson(ex, "{\"username\":" + JsonUtil.quote(user) + "}");
    }

    // ---------- search destinations ----------
    // /api/destinations?q=beach   (empty q = show everything)
    private static void destinationsApi(HttpExchange ex) throws IOException {
        if (currentUser(ex) == null) {
            sendError(ex, 401, "Please log in.");
            return;
        }
        String q = param(ex, "q").trim();

        // 1. KMP: keep every destination whose name, country, description or hotel name contains the text
        List<Destination> matches = new ArrayList<>();
        for (Destination d : destinations) {
            boolean hit = q.isEmpty()
                    || !KMP.searchIgnoreCase(d.getName(), q).isEmpty()
                    || !KMP.searchIgnoreCase(d.getCountry(), q).isEmpty()
                    || !KMP.searchIgnoreCase(d.getDescription(), q).isEmpty();
            for (Hotel h : hotels) {
                if (h.getDestinationId() == d.getId() && !KMP.searchIgnoreCase(h.getName(), q).isEmpty()) {
                    hit = true;
                }
            }
            if (hit) matches.add(d);
        }

        // 2. Randomized QuickSort: most searched destinations first
        RandomizedQuickSort<Destination> sorter = new RandomizedQuickSort<>(
                (a, b) -> Integer.compare(b.getSearchCount(), a.getSearchCount()));
        sorter.sort(matches);

        // 3. Damerau-Levenshtein: nothing found? maybe it was a typo -> suggest the closest name
        String suggestion = null;
        if (matches.isEmpty() && !q.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Destination d : destinations) names.add(d.getName());
            String best = EditDistance.suggest(q, names);
            if (best != null && EditDistance.damerau(q.toLowerCase(), best.toLowerCase()) <= 3) {
                suggestion = best;
            }
        }

        StringBuilder sb = new StringBuilder("{\"results\":[");
        for (int i = 0; i < matches.size(); i++) {
            Destination d = matches.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(d.getId())
              .append(",\"name\":").append(JsonUtil.quote(d.getName()))
              .append(",\"country\":").append(JsonUtil.quote(d.getCountry()))
              .append(",\"description\":").append(JsonUtil.quote(d.getDescription()))
              .append(",\"searches\":").append(d.getSearchCount())
              .append(",\"hotels\":[");
            boolean first = true;
            for (Hotel h : hotels) {
                if (h.getDestinationId() != d.getId()) continue;
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"id\":").append(h.getId())
                  .append(",\"name\":").append(JsonUtil.quote(h.getName()))
                  .append(",\"description\":").append(JsonUtil.quote(h.getDescription()))
                  .append(",\"price\":").append(Math.round(h.getPricePerNight()))
                  .append("}");
            }
            sb.append("]}");
        }
        sb.append("],\"suggestion\":")
          .append(suggestion == null ? "null" : JsonUtil.quote(suggestion))
          .append("}");
        sendJson(ex, sb.toString());
    }

    // ---------- booking ----------
    private static void book(HttpExchange ex) throws IOException {
        if (!requirePost(ex)) return;
        String user = currentUser(ex);
        if (user == null) {
            sendError(ex, 401, "Please log in to book.");
            return;
        }
        Map<String, String> f = form(ex);
        int hotelId, nights;
        try {
            hotelId = Integer.parseInt(f.getOrDefault("hotelId", ""));
            nights = Integer.parseInt(f.getOrDefault("nights", ""));
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Please enter a whole number of nights.");
            return;
        }
        Hotel hotel = findHotel(hotelId);
        if (hotel == null) {
            sendError(ex, 400, "That hotel does not exist.");
            return;
        }
        if (nights < 1 || nights > 30) {
            sendError(ex, 400, "Nights must be between 1 and 30.");
            return;
        }
        Reservation r = store.addReservation(user, hotelId, nights);
        sendJson(ex, "{\"code\":" + JsonUtil.quote(r.code)
                + ",\"hotel\":" + JsonUtil.quote(hotel.getName())
                + ",\"destination\":" + JsonUtil.quote(destinationName(hotel.getDestinationId()))
                + ",\"nights\":" + nights
                + ",\"total\":" + (Math.round(hotel.getPricePerNight()) * nights) + "}");
    }

    private static void myTrips(HttpExchange ex) throws IOException {
        String user = currentUser(ex);
        if (user == null) {
            sendError(ex, 401, "Please log in.");
            return;
        }
        List<Reservation> mine = store.forUser(user);
        StringBuilder sb = new StringBuilder("[");
        for (int i = mine.size() - 1; i >= 0; i--) {          // newest first
            Reservation r = mine.get(i);
            Hotel h = findHotel(r.hotelId);
            if (i < mine.size() - 1) sb.append(",");
            sb.append("{\"code\":").append(JsonUtil.quote(r.code))
              .append(",\"hotel\":").append(JsonUtil.quote(h == null ? "?" : h.getName()))
              .append(",\"destination\":").append(JsonUtil.quote(h == null ? "?" : destinationName(h.getDestinationId())))
              .append(",\"nights\":").append(r.nights)
              .append(",\"total\":").append(h == null ? 0 : Math.round(h.getPricePerNight()) * r.nights)
              .append(",\"createdAt\":").append(r.createdAt)
              .append("}");
        }
        sb.append("]");
        sendJson(ex, sb.toString());
    }

    // ---------- live feed (the page asks for this every few seconds) ----------
    private static void feed(HttpExchange ex) throws IOException {
        if (currentUser(ex) == null) {
            sendError(ex, 401, "Please log in.");
            return;
        }
        List<Reservation> all = store.all();

        long[] nights = new long[all.size()];
        long[] value = new long[all.size()];
        int[] perDestination = new int[destinations.size()];
        for (int i = 0; i < all.size(); i++) {
            Reservation r = all.get(i);
            Hotel h = findHotel(r.hotelId);
            nights[i] = r.nights;
            value[i] = (h == null) ? 0 : Math.round(h.getPricePerNight()) * r.nights;
            if (h != null) {
                for (int d = 0; d < destinations.size(); d++) {
                    if (destinations.get(d).getId() == h.getDestinationId()) perDestination[d]++;
                }
            }
        }
        // Parallel Reduce (M6) adds up the totals
        long totalNights = ParallelReduce.sum(nights);
        long totalValue = ParallelReduce.sum(value);

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"now\":").append(System.currentTimeMillis());
        sb.append(",\"totalBookings\":").append(all.size());
        sb.append(",\"totalNights\":").append(totalNights);
        sb.append(",\"totalValue\":").append(totalValue);

        sb.append(",\"byDestination\":[");
        for (int d = 0; d < destinations.size(); d++) {
            if (d > 0) sb.append(",");
            sb.append("{\"name\":").append(JsonUtil.quote(destinations.get(d).getName()))
              .append(",\"count\":").append(perDestination[d]).append("}");
        }
        sb.append("]");

        sb.append(",\"recent\":[");
        int shown = 0;
        for (int i = all.size() - 1; i >= 0 && shown < 8; i--, shown++) {   // newest first
            Reservation r = all.get(i);
            Hotel h = findHotel(r.hotelId);
            if (shown > 0) sb.append(",");
            sb.append("{\"code\":").append(JsonUtil.quote(r.code))
              .append(",\"user\":").append(JsonUtil.quote(r.username))
              .append(",\"hotel\":").append(JsonUtil.quote(h == null ? "?" : h.getName()))
              .append(",\"destination\":").append(JsonUtil.quote(h == null ? "?" : destinationName(h.getDestinationId())))
              .append(",\"nights\":").append(r.nights)
              .append(",\"createdAt\":").append(r.createdAt)
              .append("}");
        }
        sb.append("]}");
        sendJson(ex, sb.toString());
    }

    // adds a few random bookings so the live feed visibly moves during a demo
    private static void simulate(HttpExchange ex) throws IOException {
        if (!requirePost(ex)) return;
        if (currentUser(ex) == null) {
            sendError(ex, 401, "Please log in.");
            return;
        }
        int count = 5;
        try {
            count = Integer.parseInt(form(ex).getOrDefault("count", "5"));
        } catch (NumberFormatException e) {
            // keep 5
        }
        count = Math.max(1, Math.min(count, 20));

        String[] guests = {"Aarav", "Diya", "Kabir", "Isha", "Rohan", "Sneha", "Vikram", "Tara"};
        Random rng = new Random();
        for (int i = 0; i < count; i++) {
            Hotel h = hotels.get(rng.nextInt(hotels.size()));
            store.addReservation(guests[rng.nextInt(guests.length)], h.getId(), 1 + rng.nextInt(7));
        }
        sendJson(ex, "{\"added\":" + count + "}");
    }

    private static Hotel findHotel(int id) {
        for (Hotel h : hotels) {
            if (h.getId() == id) return h;
        }
        return null;
    }

    private static String destinationName(int destinationId) {
        for (Destination d : destinations) {
            if (d.getId() == destinationId) return d.getName();
        }
        return "?";
    }

    // =====================================================================
    //  ALGORITHM LAB
    // =====================================================================

    // ================= M1 =================
    private static void kmpSearch(HttpExchange ex) throws IOException {
        String q = param(ex, "q");
        List<String> found = new ArrayList<>();
        for (Destination d : destinations) {
            if (!KMP.searchIgnoreCase(d.getDescription(), q).isEmpty()) {
                found.add(d.getName());
            }
        }
        sendJson(ex, JsonUtil.arrayOfStrings(found));
    }

    // now searches ALL saved bookings, including the ones you just made
    private static void rabinKarpSearch(HttpExchange ex) throws IOException {
        String q = param(ex, "q");
        List<Booking> found = RabinKarp.findBookings(store.asBookings(), q);
        List<String> lines = new ArrayList<>();
        for (Booking b : found) lines.add(b.getCode() + " - " + b.getCustomerName());
        sendJson(ex, JsonUtil.arrayOfStrings(lines));
    }

    // ================= M2 =================
    private static void suffixSearch(HttpExchange ex) throws IOException {
        String q = param(ex, "q");
        sendJson(ex, JsonUtil.arrayOfStrings(travelIndex.search(q)));
    }

    private static void similar(HttpExchange ex) throws IOException {
        String result = CommonDescriptions.mostSimilarPair(destinations);
        sendJson(ex, "{\"result\":" + JsonUtil.quote(result) + "}");
    }

    // ================= M3 =================
    private static void spellCheck(HttpExchange ex) throws IOException {
        String q = param(ex, "q");
        List<String> names = new ArrayList<>();
        for (Destination d : destinations) names.add(d.getName());
        String best = EditDistance.suggest(q, names);
        int dist = EditDistance.damerau(q.toLowerCase(), best.toLowerCase());
        String json = "{\"input\":" + JsonUtil.quote(q)
                + ",\"suggestion\":" + JsonUtil.quote(best)
                + ",\"distance\":" + dist + "}";
        sendJson(ex, json);
    }

    private static void trip(HttpExchange ex) throws IOException {
        int[][] dist = {
            {0, 12, 8, 10, 6},
            {12, 0, 15, 14, 9},
            {8, 15, 0, 7, 11},
            {10, 14, 7, 0, 13},
            {6, 9, 11, 13, 0}
        };
        String[] names5 = {"Goa", "Paris", "Bali", "Kyoto", "Manali"};
        ItineraryPlanner.Result r = ItineraryPlanner.bestRoute(dist, 0, 3);
        List<String> routeNames = new ArrayList<>();
        for (int idx : r.route) routeNames.add(names5[idx]);
        String json = "{\"cost\":" + r.cost + ",\"route\":" + JsonUtil.arrayOfStrings(routeNames) + "}";
        sendJson(ex, json);
    }

    // ================= M4 =================
    private static FlowNetwork buildNetwork() {
        FlowNetwork net = new FlowNetwork(6);
        net.addLink(0, 1, 16);
        net.addLink(0, 2, 13);
        net.addLink(1, 3, 12);
        net.addLink(2, 1, 4);
        net.addLink(2, 4, 14);
        net.addLink(3, 2, 9);
        net.addLink(3, 5, 20);
        net.addLink(4, 3, 7);
        net.addLink(4, 5, 4);
        return net;
    }

    private static void flow(HttpExchange ex) throws IOException {
        FlowNetwork net = buildNetwork();
        int ff = MaxFlow.fordFulkerson(net, 0, 5);
        int ek = MaxFlow.edmondsKarp(net, 0, 5);
        int dn = MaxFlow.dinic(net, 0, 5);
        String json = "{\"fordFulkerson\":" + ff + ",\"edmondsKarp\":" + ek + ",\"dinic\":" + dn + "}";
        sendJson(ex, json);
    }

    private static void minCut(HttpExchange ex) throws IOException {
        FlowNetwork net = buildNetwork();
        MaxFlow.CutResult cut = MaxFlow.minCut(net, 0, 5);
        List<String> edgeStrings = new ArrayList<>();
        for (int[] e : cut.edges) {
            edgeStrings.add(e[0] + " -> " + e[1] + " (capacity " + e[2] + ")");
        }
        String json = "{\"value\":" + cut.value + ",\"edges\":" + JsonUtil.arrayOfStrings(edgeStrings) + "}";
        sendJson(ex, json);
    }

    private static void match(HttpExchange ex) throws IOException {
        boolean[][] eligible = {
            { true,  true,  false },
            { true,  false, false },
            { false, true,  true  },
            { false, false, true  }
        };
        int[] result = BipartiteMatching.match(eligible, 3);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < result.length; i++) {
            lines.add("Traveler " + i + " -> " + (result[i] == -1 ? "no hotel" : "Hotel " + result[i]));
        }
        sendJson(ex, JsonUtil.arrayOfStrings(lines));
    }

    // ================= M5 =================
    // /api/np?case=feasible  or  /api/np?case=conflict
    private static void np(HttpExchange ex) throws IOException {
        boolean feasible = !"conflict".equals(param(ex, "case"));
        sendText(ex, NpDemo.run(feasible));
    }

    private static void vertexCover(HttpExchange ex) throws IOException {
        sendText(ex, NpDemo.vertexCover());
    }

    // ================= M6 =================
    // /api/rank?by=popularity  or  /api/rank?by=price
    private static void rank(HttpExchange ex) throws IOException {
        sendText(ex, RandomDemo.rank(param(ex, "by")));
    }

    // /api/reservoir?mode=sample  or  /api/reservoir?mode=fairness
    private static void reservoir(HttpExchange ex) throws IOException {
        sendText(ex, RandomDemo.reservoir(param(ex, "mode")));
    }

    private static void prime(HttpExchange ex) throws IOException {
        sendText(ex, RandomDemo.prime(param(ex, "n")));
    }

    private static void scan(HttpExchange ex) throws IOException {
        sendText(ex, RandomDemo.scan());
    }

    private static void reduce(HttpExchange ex) throws IOException {
        sendText(ex, RandomDemo.reduce());
    }

    private static void brent(HttpExchange ex) throws IOException {
        sendText(ex, RandomDemo.brent(param(ex, "n")));
    }

    // =====================================================================
    //  helpers
    // =====================================================================

    // who is logged in? The browser sends its login token in the X-Token header.
    private static String currentUser(HttpExchange ex) {
        String token = ex.getRequestHeaders().getFirst("X-Token");
        if (token == null || token.isEmpty()) return null;
        return store.usernameForToken(token);
    }

    private static boolean requirePost(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            sendError(ex, 405, "Use POST for this request.");
            return false;
        }
        return true;
    }

    // reads a form sent with POST, e.g.  username=anita&password=1234
    private static Map<String, String> form(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static String param(HttpExchange ex, String key) {
        String query = ex.getRequestURI().getQuery();
        if (query == null) return "";
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static void sendJson(HttpExchange ex, String json) throws IOException {
        sendJson(ex, 200, json);
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange ex, int status, String message) throws IOException {
        sendJson(ex, status, "{\"error\":" + JsonUtil.quote(message) + "}");
    }

    // sends a ready-made multi-line text answer as {"text": "..."}
    private static void sendText(HttpExchange ex, String text) throws IOException {
        sendJson(ex, "{\"text\":" + JsonUtil.quote(text) + "}");
    }

    private static void staticFile(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path file = Path.of("frontend" + path);

        if (!Files.exists(file)) {
            String msg = "404 Not Found: " + path;
            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
            return;
        }

        String contentType = "text/plain";
        if (path.endsWith(".html")) contentType = "text/html";
        else if (path.endsWith(".css")) contentType = "text/css";
        else if (path.endsWith(".js")) contentType = "application/javascript";

        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}