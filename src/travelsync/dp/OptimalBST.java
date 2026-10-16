package travelsync.dp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import travelsync.model.Destination;

public class OptimalBST {

    public static class Result {
        public long cost;
        public String tree;
    }

    // keys must be in sorted order; freq[i] = how often key i is searched
    public static Result solve(String[] keys, int[] freq) {
        int n = keys.length;
        long[] pre = new long[n + 1];
        for (int i = 1; i <= n; i++) pre[i] = pre[i - 1] + freq[i - 1];

        long[][] e = new long[n + 2][n + 1];
        int[][] root = new int[n + 2][n + 1];

        for (int len = 1; len <= n; len++) {
            for (int i = 1; i + len - 1 <= n; i++) {
                int j = i + len - 1;
                e[i][j] = Long.MAX_VALUE;
                long sum = pre[j] - pre[i - 1];
                for (int r = i; r <= j; r++) {
                    long cost = e[i][r - 1] + e[r + 1][j] + sum;
                    if (cost < e[i][j]) {
                        e[i][j] = cost;
                        root[i][j] = r;
                    }
                }
            }
        }

        Result res = new Result();
        res.cost = e[1][n];
        res.tree = draw(keys, root, 1, n);
        return res;
    }

    private static String draw(String[] keys, int[][] root, int i, int j) {
        if (i > j) return "-";
        int r = root[i][j];
        String left = draw(keys, root, i, r - 1);
        String right = draw(keys, root, r + 1, j);
        if (left.equals("-") && right.equals("-")) return keys[r - 1];
        return keys[r - 1] + "(" + left + "," + right + ")";
    }

    // Build the best search tree for destinations using their search counts
    public static Result forDestinations(List<Destination> list) {
        List<Destination> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing(Destination::getName));
        String[] keys = new String[sorted.size()];
        int[] freq = new int[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            keys[i] = sorted.get(i).getName();
            freq[i] = sorted.get(i).getSearchCount();
        }
        return solve(keys, freq);
    }
}