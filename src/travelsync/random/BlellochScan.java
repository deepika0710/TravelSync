package travelsync.random;

import java.util.Arrays;
import java.util.stream.IntStream;

public class BlellochScan {

    public static class Result {
        public long[] exclusive;   // total of everything BEFORE each position
        public long[] inclusive;   // running total INCLUDING each position
        public String log;         // the steps, for the dashboard
    }

    // Work-efficient parallel prefix sum. Inside each step, every pair is independent,
    // so they can be done at the same time (we use parallel streams for that).
    public static Result scan(long[] input) {
        int n = input.length;
        int size = 1;
        while (size < n) size <<= 1;               // pad up to a power of two
        long[] a = Arrays.copyOf(input, size);
        StringBuilder log = new StringBuilder();
        log.append(String.format("%-15s", "Start:")).append(Arrays.toString(a)).append("\n");

        // Phase 1: up-sweep - build partial sums like a tournament tree
        for (int step = 1; step < size; step <<= 1) {
            final int s = step;
            int groups = size / (2 * s);
            IntStream.range(0, groups).parallel().forEach(g -> {
                int right = (g + 1) * 2 * s - 1;
                int left = right - s;
                a[right] += a[left];
            });
            log.append(String.format("%-15s", "Up-sweep " + step + ":")).append(Arrays.toString(a)).append("\n");
        }

        // The grand total is now in the last slot; clear it to start the down-sweep
        a[size - 1] = 0;
        log.append(String.format("%-15s", "Clear last:")).append(Arrays.toString(a)).append("\n");

        // Phase 2: down-sweep - push the totals back down to every position
        for (int step = size / 2; step >= 1; step >>= 1) {
            final int s = step;
            int groups = size / (2 * s);
            IntStream.range(0, groups).parallel().forEach(g -> {
                int right = (g + 1) * 2 * s - 1;
                int left = right - s;
                long t = a[left];
                a[left] = a[right];
                a[right] += t;
            });
            log.append(String.format("%-15s", "Down-sweep " + step + ":")).append(Arrays.toString(a)).append("\n");
        }

        Result r = new Result();
        r.exclusive = Arrays.copyOf(a, n);
        r.inclusive = new long[n];
        for (int i = 0; i < n; i++) r.inclusive[i] = r.exclusive[i] + input[i];
        r.log = log.toString();
        return r;
    }
}