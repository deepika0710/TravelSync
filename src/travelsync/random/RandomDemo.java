package travelsync.random;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import travelsync.model.Destination;
import travelsync.model.Hotel;
import travelsync.model.SampleData;

public class RandomDemo {

    // ---------- Randomized QuickSort: rank destinations by what the user prefers ----------
    public static String rank(String by) {
        List<Destination> list = new ArrayList<>(SampleData.destinations());

        Map<Integer, Double> cheapest = new HashMap<>();
        for (Hotel h : SampleData.hotels()) {
            cheapest.merge(h.getDestinationId(), h.getPricePerNight(), Math::min);
        }

        boolean byPrice = "price".equals(by);
        Comparator<Destination> cmp;
        if (byPrice) {
            cmp = Comparator.comparingDouble(d -> cheapest.getOrDefault(d.getId(), Double.MAX_VALUE));
        } else {
            cmp = (a, b) -> Integer.compare(b.getSearchCount(), a.getSearchCount());
        }

        RandomizedQuickSort<Destination> qs = new RandomizedQuickSort<>(cmp);
        qs.sort(list);

        StringBuilder sb = new StringBuilder();
        sb.append(byPrice ? "Ranking by: cheapest hotel first\n" : "Ranking by: most searched first\n");
        for (int i = 0; i < list.size(); i++) {
            Destination d = list.get(i);
            sb.append(i + 1).append(". ").append(d.getName()).append(" - ");
            if (byPrice) {
                sb.append(String.format(Locale.US, "from %.0f per night", cheapest.get(d.getId())));
            } else {
                sb.append(d.getSearchCount()).append(" searches");
            }
            sb.append("\n");
        }
        sb.append("\nPivots were picked at random; this run used ").append(qs.comparisons).append(" comparisons.\n");
        sb.append("Click again: the ranking stays the same, the comparison count can change.");
        return sb.toString();
    }

    // ---------- Reservoir Sampling: keep a fair sample of a never-ending booking stream ----------
    public static String reservoir(String mode) {
        Random rng = new Random();
        StringBuilder sb = new StringBuilder();

        if ("fairness".equals(mode)) {
            int items = 10, k = 3, trials = 20000;
            int[] counts = new int[items];
            for (int t = 0; t < trials; t++) {
                ReservoirSampler<Integer> s = new ReservoirSampler<>(k, rng);
                for (int i = 0; i < items; i++) s.add(i);
                for (int x : s.getSample()) counts[x]++;
            }
            sb.append("Fairness check: stream of ").append(items).append(" bookings, keep ").append(k)
              .append(", repeated ").append(trials).append(" times.\n");
            sb.append("Every booking should be kept about 30% of the time (3 of 10):\n");
            for (int i = 0; i < items; i++) {
                sb.append(String.format(Locale.US, "  Booking %2d: %.1f%%\n", i + 1, 100.0 * counts[i] / trials));
            }
            return sb.toString().trim();
        }

        int streamSize = 1000, k = 5;
        ReservoirSampler<String> s = new ReservoirSampler<>(k, rng);
        for (int i = 1; i <= streamSize; i++) {
            s.add(String.format(Locale.US, "BK-2026-%05d", i));
        }
        sb.append("Bookings that streamed past: ").append(s.getSeen()).append("\n");
        sb.append("Memory used: only ").append(k).append(" slots\n\n");
        sb.append("Random sample kept:\n");
        for (String code : s.getSample()) sb.append("  ").append(code).append("\n");
        sb.append("\nClick again for a different fair sample.");
        return sb.toString();
    }

    // ---------- Miller-Rabin ----------
    public static String prime(String text) {
        long n;
        try {
            n = Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return "Please enter a whole number (up to 18 digits), for example 1000000007.";
        }
        return MillerRabin.explain(n);
    }

    // ---------- Blelloch Scan ----------
    public static String scan() {
        long[] daily = {3, 1, 7, 0, 4, 1, 6, 3};
        BlellochScan.Result r = BlellochScan.scan(daily);
        StringBuilder sb = new StringBuilder();
        sb.append("Bookings per day (day 1 to 8): ").append(java.util.Arrays.toString(daily)).append("\n\n");
        sb.append(r.log).append("\n");
        sb.append(String.format("%-15s", "Before day:")).append(java.util.Arrays.toString(r.exclusive)).append("\n");
        sb.append(String.format("%-15s", "Total so far:")).append(java.util.Arrays.toString(r.inclusive));
        return sb.toString();
    }

    // ---------- Parallel Reduce ----------
    public static String reduce() {
        Random rng = new Random(42);
        long[] demand = new long[1_000_000];
        for (int i = 0; i < demand.length; i++) demand[i] = 1 + rng.nextInt(10); // seats per request

        long seq = ParallelReduce.sequentialSum(demand);
        long par = ParallelReduce.sum(demand);
        long peak = ParallelReduce.max(demand);

        StringBuilder sb = new StringBuilder();
        sb.append("Simulated 1,000,000 booking requests (1 to 10 seats each).\n\n");
        sb.append(String.format(Locale.US, "Total seats requested (one worker):   %,d\n", seq));
        sb.append(String.format(Locale.US, "Total seats requested (many workers): %,d\n", par));
        sb.append("Both answers match: ").append(seq == par).append("\n");
        sb.append("Biggest single request: ").append(peak).append(" seats\n");
        sb.append("Processor cores available here: ").append(Runtime.getRuntime().availableProcessors());
        return sb.toString();
    }

    // ---------- Brent's theorem ----------
    public static String brent(String text) {
        long n;
        try {
            n = text.trim().isEmpty() ? 1_000_000L : Long.parseLong(text.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return "Please enter a whole number of data items, for example 1000000.";
        }
        if (n < 2 || n > 1_000_000_000_000L) {
            return "Please enter a number between 2 and 1,000,000,000,000.";
        }

        int logN = Brent.log2Ceil(n);
        int[] procs = {1, 2, 4, 8, 16, 64, 1024};
        StringBuilder sb = new StringBuilder();
        sb.append("Brent's theorem: time with p processors is at most  W/p + D\n");
        sb.append("  W = total work (all the steps), D = depth (steps that must happen in order)\n");
        sb.append("  Sequential baseline for both jobs: ").append(String.format(Locale.US, "%,d", n - 1)).append(" steps\n");

        appendTable(sb, "Adding up " + String.format(Locale.US, "%,d", n) + " booking counts (parallel reduce)",
                n - 1, logN, n - 1, procs);
        appendTable(sb, "Running totals of " + String.format(Locale.US, "%,d", n) + " counts (Blelloch scan)",
                2 * (n - 1), 2 * logN, n - 1, procs);

        sb.append("\nTakeaway: more processors help a lot at first, but the depth D never shrinks,\n");
        sb.append("so the speedup flattens out. The scan does about twice the work of a plain sum.");
        return sb.toString();
    }

    private static void appendTable(StringBuilder sb, String title, long work, int depth, long sequential, int[] procs) {
        sb.append("\n").append(title).append("\n");
        sb.append(String.format(Locale.US, "  W = %,d, D = %d\n", work, depth));
        for (int p : procs) {
            double t = Brent.bound(work, depth, p);
            sb.append(String.format(Locale.US, "  p = %-4d at most %,.0f steps  (speedup at least %.1fx)\n",
                    p, t, sequential / t));
        }
    }
}