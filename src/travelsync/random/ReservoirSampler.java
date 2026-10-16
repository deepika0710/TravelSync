package travelsync.random;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReservoirSampler<T> {

    private final int k;                       // how many items we keep
    private final List<T> reservoir = new ArrayList<>();
    private final Random rng;
    private long seen = 0;                     // how many items have passed by so far

    public ReservoirSampler(int k, Random rng) {
        this.k = k;
        this.rng = rng;
    }

    // Call this once for every new booking that arrives. Memory never grows beyond k items.
    public void add(T item) {
        seen++;
        if (reservoir.size() < k) {
            reservoir.add(item);                       // first k items always get in
        } else {
            long j = (long) (rng.nextDouble() * seen); // random number from 0 to seen-1
            if (j < k) {
                reservoir.set((int) j, item);          // chance k/seen to replace an old one
            }
        }
    }

    public List<T> getSample() {
        return new ArrayList<>(reservoir);
    }

    public long getSeen() {
        return seen;
    }
}