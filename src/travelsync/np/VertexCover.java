package travelsync.np;

import java.util.ArrayList;
import java.util.List;

public class VertexCover {

    public static class Result {
        public List<Integer> cover = new ArrayList<>();
        public List<int[]> pickedEdges = new ArrayList<>();
    }

    // 2-approximation: take any connection that is not covered yet, put BOTH its ends in the cover, repeat.
    // The answer is never more than twice the size of the best possible cover.
    public static Result approx(int n, List<int[]> edges) {
        boolean[] in = new boolean[n];
        Result r = new Result();
        for (int[] e : edges) {
            if (!in[e[0]] && !in[e[1]]) {
                in[e[0]] = true;
                in[e[1]] = true;
                r.pickedEdges.add(e);
            }
        }
        for (int v = 0; v < n; v++) {
            if (in[v]) r.cover.add(v);
        }
        return r;
    }

    // Smallest possible cover by trying every group of cities (only for small n, used to compare).
    public static List<Integer> exact(int n, List<int[]> edges) {
        int bestMask = -1;
        int bestSize = Integer.MAX_VALUE;
        for (int mask = 0; mask < (1 << n); mask++) {
            int size = Integer.bitCount(mask);
            if (size >= bestSize) continue;
            boolean ok = true;
            for (int[] e : edges) {
                if ((mask & (1 << e[0])) == 0 && (mask & (1 << e[1])) == 0) { ok = false; break; }
            }
            if (ok) {
                bestSize = size;
                bestMask = mask;
            }
        }
        List<Integer> cover = new ArrayList<>();
        for (int v = 0; v < n; v++) {
            if ((bestMask & (1 << v)) != 0) cover.add(v);
        }
        return cover;
    }
}