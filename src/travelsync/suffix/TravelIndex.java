package travelsync.suffix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import travelsync.model.Destination;
import travelsync.model.Hotel;

public class TravelIndex {

    private final String text;
    private final int[] sa;
    private final List<Integer> starts = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();

    public TravelIndex(List<Destination> destinations, List<Hotel> hotels) {
        StringBuilder sb = new StringBuilder();
        for (Destination d : destinations) {
            starts.add(sb.length());
            labels.add("Destination: " + d.getName());
            sb.append(d.getDescription().toLowerCase()).append('\u0001');
        }
        for (Hotel h : hotels) {
            starts.add(sb.length());
            labels.add("Hotel: " + h.getName());
            sb.append(h.getDescription().toLowerCase()).append('\u0001');
        }
        text = sb.toString();
        sa = SuffixArray.build(text);
    }

    // returns the names of all destinations/hotels whose description contains the query
    public List<String> search(String query) {
        Set<String> found = new LinkedHashSet<>();
        for (int pos : SuffixArray.search(text, sa, query.toLowerCase())) {
            found.add(labels.get(docIndex(pos)));
        }
        return new ArrayList<>(found);
    }

    private int docIndex(int pos) {
        int i = Collections.binarySearch(starts, pos);
        return i >= 0 ? i : -i - 2;
    }
}