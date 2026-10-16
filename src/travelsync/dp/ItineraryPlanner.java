package travelsync.dp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItineraryPlanner {

    public static class Result {
        public int cost;
        public List<Integer> route;
    }

    // Best route that starts at 'start' and visits exactly k destinations in total
    // dist[i][j] = travel cost (e.g. hours) from destination i to j. Keep n small (up to ~15).
    public static Result bestRoute(int[][] dist, int start, int k) {
        int n = dist.length;
        if (k < 1 || k > n) return null;

        int full = 1 << n;
        final int INF = Integer.MAX_VALUE / 2;
        int[][] dp = new int[full][n];      // dp[mask][last] = cheapest cost
        int[][] parent = new int[full][n];
        for (int mask = 0; mask < full; mask++) {
            for (int j = 0; j < n; j++) {
                dp[mask][j] = INF;
                parent[mask][j] = -1;
            }
        }
        dp[1 << start][start] = 0;

        for (int mask = 0; mask < full; mask++) {
            if (Integer.bitCount(mask) >= k) continue; // already big enough, do not extend
            for (int last = 0; last < n; last++) {
                if (dp[mask][last] >= INF) continue;
                for (int nxt = 0; nxt < n; nxt++) {
                    if ((mask & (1 << nxt)) != 0) continue; // already visited
                    int newMask = mask | (1 << nxt);
                    int cost = dp[mask][last] + dist[last][nxt];
                    if (cost < dp[newMask][nxt]) {
                        dp[newMask][nxt] = cost;
                        parent[newMask][nxt] = last;
                    }
                }
            }
        }

        int bestCost = INF, bestMask = -1, bestLast = -1;
        for (int mask = 0; mask < full; mask++) {
            if (Integer.bitCount(mask) != k) continue;
            for (int last = 0; last < n; last++) {
                if (dp[mask][last] < bestCost) {
                    bestCost = dp[mask][last];
                    bestMask = mask;
                    bestLast = last;
                }
            }
        }
        if (bestMask == -1) return null;

        Result res = new Result();
        res.cost = bestCost;
        res.route = new ArrayList<>();
        int mask = bestMask, last = bestLast;
        while (last != -1) {
            res.route.add(last);
            int prev = parent[mask][last];
            mask ^= (1 << last);
            last = prev;
        }
        Collections.reverse(res.route);
        return res;
    }
}