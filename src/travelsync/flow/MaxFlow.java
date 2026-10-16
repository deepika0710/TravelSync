package travelsync.flow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import travelsync.flow.FlowNetwork.Edge;

public class MaxFlow {

    // ---------- 1. Ford-Fulkerson (finds any path using DFS) ----------
    public static int fordFulkerson(FlowNetwork net, int s, int t) {
        List<List<Edge>> g = net.buildResidual();
        int flow = 0;
        while (true) {
            boolean[] seen = new boolean[net.size()];
            int pushed = dfs(g, s, t, Integer.MAX_VALUE, seen);
            if (pushed == 0) break;
            flow += pushed;
        }
        return flow;
    }

    private static int dfs(List<List<Edge>> g, int u, int t, int f, boolean[] seen) {
        if (u == t) return f;
        seen[u] = true;
        for (Edge e : g.get(u)) {
            if (!seen[e.to] && e.cap > 0) {
                int d = dfs(g, e.to, t, Math.min(f, e.cap), seen);
                if (d > 0) {
                    e.cap -= d;
                    g.get(e.to).get(e.rev).cap += d;
                    return d;
                }
            }
        }
        return 0;
    }

    // ---------- 2. Edmonds-Karp (always the path with fewest stops, using BFS) ----------
    public static int edmondsKarp(FlowNetwork net, int s, int t) {
        int n = net.size();
        List<List<Edge>> g = net.buildResidual();
        int flow = 0;
        while (true) {
            int[] prevNode = new int[n];
            int[] prevEdge = new int[n];
            Arrays.fill(prevNode, -1);
            prevNode[s] = s;
            Queue<Integer> queue = new ArrayDeque<>();
            queue.add(s);
            while (!queue.isEmpty() && prevNode[t] == -1) {
                int u = queue.poll();
                for (int i = 0; i < g.get(u).size(); i++) {
                    Edge e = g.get(u).get(i);
                    if (prevNode[e.to] == -1 && e.cap > 0) {
                        prevNode[e.to] = u;
                        prevEdge[e.to] = i;
                        queue.add(e.to);
                    }
                }
            }
            if (prevNode[t] == -1) break; // no more paths

            int bottleneck = Integer.MAX_VALUE;
            for (int v = t; v != s; v = prevNode[v]) {
                bottleneck = Math.min(bottleneck, g.get(prevNode[v]).get(prevEdge[v]).cap);
            }
            for (int v = t; v != s; v = prevNode[v]) {
                Edge e = g.get(prevNode[v]).get(prevEdge[v]);
                e.cap -= bottleneck;
                g.get(v).get(e.rev).cap += bottleneck;
            }
            flow += bottleneck;
        }
        return flow;
    }

    // ---------- 3. Dinic (layers + many pushes at once) ----------
    public static int dinic(FlowNetwork net, int s, int t) {
        return dinicOn(net.buildResidual(), net.size(), s, t);
    }

    private static int dinicOn(List<List<Edge>> g, int n, int s, int t) {
        int flow = 0;
        int[] level = new int[n];
        while (buildLevels(g, s, t, level)) {
            int[] iter = new int[n];
            int f;
            while ((f = dinicDfs(g, s, t, Integer.MAX_VALUE, level, iter)) > 0) {
                flow += f;
            }
        }
        return flow;
    }

    private static boolean buildLevels(List<List<Edge>> g, int s, int t, int[] level) {
        Arrays.fill(level, -1);
        level[s] = 0;
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(s);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (Edge e : g.get(u)) {
                if (e.cap > 0 && level[e.to] < 0) {
                    level[e.to] = level[u] + 1;
                    queue.add(e.to);
                }
            }
        }
        return level[t] >= 0;
    }

    private static int dinicDfs(List<List<Edge>> g, int u, int t, int f, int[] level, int[] iter) {
        if (u == t) return f;
        for (; iter[u] < g.get(u).size(); iter[u]++) {
            Edge e = g.get(u).get(iter[u]);
            if (e.cap > 0 && level[u] < level[e.to]) {
                int d = dinicDfs(g, e.to, t, Math.min(f, e.cap), level, iter);
                if (d > 0) {
                    e.cap -= d;
                    g.get(e.to).get(e.rev).cap += d;
                    return d;
                }
            }
        }
        return 0;
    }

    // ---------- 4. Min-Cut (the bottleneck links) ----------
    public static class CutResult {
        public int value;
        public List<int[]> edges = new ArrayList<>(); // {from, to, capacity}

        public String edgesToString() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < edges.size(); i++) {
                int[] e = edges.get(i);
                if (i > 0) sb.append(", ");
                sb.append(e[0]).append("->").append(e[1]).append(" (").append(e[2]).append(")");
            }
            return sb.append("]").toString();
        }
    }

    public static CutResult minCut(FlowNetwork net, int s, int t) {
        int n = net.size();
        List<List<Edge>> g = net.buildResidual();
        CutResult result = new CutResult();
        result.value = dinicOn(g, n, s, t);

        // every city still reachable from s (through links with spare room)
        boolean[] reach = new boolean[n];
        Queue<Integer> queue = new ArrayDeque<>();
        reach[s] = true;
        queue.add(s);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (Edge e : g.get(u)) {
                if (e.cap > 0 && !reach[e.to]) {
                    reach[e.to] = true;
                    queue.add(e.to);
                }
            }
        }
        // original links that jump from the reachable side to the unreachable side = bottleneck
        for (int[] l : net.getLinks()) {
            if (reach[l[0]] && !reach[l[1]]) result.edges.add(l);
        }
        return result;
    }
}