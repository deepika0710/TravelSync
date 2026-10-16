package travelsync.np;

import java.util.ArrayList;
import java.util.List;

public class SatReductions {

    public static class Graph {
        public int n;                // number of vertices
        public boolean[][] adj;      // adj[u][v] = true if there is an edge
        public int[] clauseOf;       // which clause vertex v belongs to
        public int[] literalOf;      // which literal vertex v stands for
    }

    // 3-SAT -> CLIQUE
    // One vertex per literal in every clause. Two vertices are joined if they come from
    // DIFFERENT clauses and do not contradict each other (x and NOT x).
    // The formula is satisfiable exactly when this graph has a clique of size = number of clauses.
    public static Graph buildCliqueGraph(List<int[]> clauses) {
        Graph g = new Graph();
        g.n = clauses.size() * 3;
        g.adj = new boolean[g.n][g.n];
        g.clauseOf = new int[g.n];
        g.literalOf = new int[g.n];
        for (int i = 0; i < clauses.size(); i++) {
            for (int j = 0; j < 3; j++) {
                g.clauseOf[3 * i + j] = i;
                g.literalOf[3 * i + j] = clauses.get(i)[j];
            }
        }
        for (int u = 0; u < g.n; u++) {
            for (int v = u + 1; v < g.n; v++) {
                boolean differentClause = g.clauseOf[u] != g.clauseOf[v];
                boolean contradict = g.literalOf[u] == -g.literalOf[v];
                if (differentClause && !contradict) {
                    g.adj[u][v] = true;
                    g.adj[v][u] = true;
                }
            }
        }
        return g;
    }

    // Looks for a clique (everyone connected to everyone) of exactly size k. Returns null if none.
    public static List<Integer> findClique(boolean[][] adj, int k) {
        List<Integer> current = new ArrayList<>();
        return search(adj, 0, k, current) ? current : null;
    }

    private static boolean search(boolean[][] adj, int start, int k, List<Integer> current) {
        if (current.size() == k) return true;
        for (int v = start; v < adj.length; v++) {
            boolean connectedToAll = true;
            for (int u : current) {
                if (!adj[u][v]) { connectedToAll = false; break; }
            }
            if (connectedToAll) {
                current.add(v);
                if (search(adj, v + 1, k, current)) return true;
                current.remove(current.size() - 1);
            }
        }
        return false;
    }

    // CLIQUE -> INDEPENDENT-SET
    // Flip the graph: connected pairs become unconnected and the other way round.
    // A clique in the original is an independent set (nobody connected) in the flipped graph.
    public static boolean[][] complement(boolean[][] adj) {
        int n = adj.length;
        boolean[][] c = new boolean[n][n];
        for (int u = 0; u < n; u++) {
            for (int v = 0; v < n; v++) {
                c[u][v] = (u != v) && !adj[u][v];
            }
        }
        return c;
    }

    public static boolean isIndependentSet(boolean[][] adj, List<Integer> set) {
        for (int i = 0; i < set.size(); i++) {
            for (int j = i + 1; j < set.size(); j++) {
                if (adj[set.get(i)][set.get(j)]) return false;
            }
        }
        return true;
    }

    // INDEPENDENT-SET -> VERTEX-COVER
    // Everything that is NOT in the independent set forms a vertex cover.
    public static List<Integer> vertexCoverFromIndependentSet(int n, List<Integer> independentSet) {
        boolean[] in = new boolean[n];
        for (int v : independentSet) in[v] = true;
        List<Integer> cover = new ArrayList<>();
        for (int v = 0; v < n; v++) {
            if (!in[v]) cover.add(v);
        }
        return cover;
    }

    public static boolean isVertexCover(boolean[][] adj, List<Integer> cover) {
        boolean[] in = new boolean[adj.length];
        for (int v : cover) in[v] = true;
        for (int u = 0; u < adj.length; u++) {
            for (int v = u + 1; v < adj.length; v++) {
                if (adj[u][v] && !in[u] && !in[v]) return false;
            }
        }
        return true;
    }
}