package travelsync.np;

import java.util.ArrayList;
import java.util.List;

public class NpDemo {

    private static final String[] PLAN_NAMES = {"Goa", "Paris", "Bali", "Kyoto"};

    // A plan that CAN work (e.g. Goa = yes, Paris = no, Kyoto = yes)
    private static List<int[]> feasibleClauses() {
        List<int[]> c = new ArrayList<>();
        c.add(new int[] { 1, 3, 4 });     // Goa OR Bali OR Kyoto
        c.add(new int[] { -1, 2, 4 });    // NOT Goa OR Paris OR Kyoto
        c.add(new int[] { -2, -3, 4 });   // NOT Paris OR NOT Bali OR Kyoto
        c.add(new int[] { -4, 1, -2 });   // NOT Kyoto OR Goa OR NOT Paris
        return c;
    }

    // Every possible combination over Goa, Paris, Bali is forbidden -> impossible to satisfy
    private static List<int[]> conflictClauses() {
        List<int[]> c = new ArrayList<>();
        for (int mask = 0; mask < 8; mask++) {
            c.add(new int[] {
                (mask & 1) != 0 ? 1 : -1,
                (mask & 2) != 0 ? 2 : -2,
                (mask & 4) != 0 ? 3 : -3 });
        }
        return c;
    }

    public static String run(boolean feasible) {
        List<int[]> clauses = feasible ? feasibleClauses() : conflictClauses();
        int numVars = feasible ? 4 : 3;
        StringBuilder sb = new StringBuilder();

        sb.append(feasible ? "PLAN A (rules that can work)\n" : "PLAN B (rules that conflict)\n");
        sb.append("Each rule needs at least one part to be true:\n");
        for (int i = 0; i < clauses.size(); i++) {
            sb.append("  Rule ").append(i + 1).append(": ")
              .append(ThreeSAT.describeClause(clauses.get(i), PLAN_NAMES)).append("\n");
        }

        // Step 1: 3-SAT
        boolean[] plan = ThreeSAT.solve(numVars, clauses);
        sb.append("\nStep 1 - 3-SAT: ");
        if (plan == null) {
            sb.append("NOT satisfiable. No choice of destinations satisfies every rule.\n");
        } else {
            sb.append("satisfiable. One valid plan: ");
            for (int v = 1; v <= numVars; v++) {
                if (v > 1) sb.append(", ");
                sb.append(PLAN_NAMES[v - 1]).append(plan[v] ? "=yes" : "=no");
            }
            sb.append("\n");
        }

        // Step 2: 3-SAT -> CLIQUE
        SatReductions.Graph g = SatReductions.buildCliqueGraph(clauses);
        int k = clauses.size();
        sb.append("\nStep 2 - 3-SAT to CLIQUE: ").append(g.n).append(" vertices (one per rule part), ")
          .append("looking for a group of ").append(k).append(" that are all connected.\n");
        List<Integer> clique = SatReductions.findClique(g.adj, k);
        if (clique == null) {
            sb.append("  No clique of size ").append(k).append(" exists (same answer as Step 1).\n");
        } else {
            sb.append("  Clique found: ");
            for (int i = 0; i < clique.size(); i++) {
                int v = clique.get(i);
                if (i > 0) sb.append(", ");
                sb.append("Rule ").append(g.clauseOf[v] + 1).append(":")
                  .append(ThreeSAT.literalName(g.literalOf[v], PLAN_NAMES));
            }
            sb.append("\n");
        }

        // Step 3: CLIQUE -> INDEPENDENT-SET
        boolean[][] flipped = SatReductions.complement(g.adj);
        sb.append("\nStep 3 - CLIQUE to INDEPENDENT-SET: flip the graph (connected <-> not connected).\n");
        if (clique == null) {
            sb.append("  No independent set of size ").append(k).append(" exists in the flipped graph.\n");
        } else {
            sb.append("  The same vertices are now an independent set (nobody connected): ")
              .append(SatReductions.isIndependentSet(flipped, clique)).append("\n");
        }

        // Step 4: INDEPENDENT-SET -> VERTEX-COVER
        sb.append("\nStep 4 - INDEPENDENT-SET to VERTEX-COVER: everything outside the independent set is a cover.\n");
        if (clique == null) {
            sb.append("  No vertex cover of size ").append(g.n - k).append(" exists.\n");
        } else {
            List<Integer> cover = SatReductions.vertexCoverFromIndependentSet(g.n, clique);
            sb.append("  Cover of size ").append(cover.size()).append(" (").append(g.n).append(" - ").append(k)
              .append("); it touches every connection: ")
              .append(SatReductions.isVertexCover(flipped, cover)).append("\n");
        }

        boolean agree = (plan != null) == (clique != null);
        sb.append("\nAll steps agree: ").append(agree);
        return sb.toString();
    }

    private static final String[] CITY = {"Goa", "Paris", "Bali", "Kyoto", "Manali", "Santorini"};

    public static String vertexCover() {
        List<int[]> edges = new ArrayList<>();
        edges.add(new int[] { 0, 2 });
        edges.add(new int[] { 0, 3 });
        edges.add(new int[] { 0, 4 });
        edges.add(new int[] { 1, 4 });
        edges.add(new int[] { 1, 5 });
        edges.add(new int[] { 2, 3 });
        edges.add(new int[] { 3, 4 });

        StringBuilder sb = new StringBuilder();
        sb.append("Popular travel connections:\n");
        for (int[] e : edges) {
            sb.append("  ").append(CITY[e[0]]).append(" - ").append(CITY[e[1]]).append("\n");
        }
        sb.append("\nGoal: choose hub cities so every connection touches at least one hub.\n");

        VertexCover.Result r = VertexCover.approx(CITY.length, edges);
        sb.append("\n2-approximation: take an uncovered connection, add BOTH its cities, repeat.\n");
        for (int[] e : r.pickedEdges) {
            sb.append("  Picked ").append(CITY[e[0]]).append(" - ").append(CITY[e[1]])
              .append(" -> hubs ").append(CITY[e[0]]).append(" and ").append(CITY[e[1]]).append("\n");
        }
        sb.append("Approximate hubs (").append(r.cover.size()).append("): ").append(names(r.cover)).append("\n");

        List<Integer> best = VertexCover.exact(CITY.length, edges);
        sb.append("Best possible hubs (").append(best.size()).append("): ").append(names(best)).append("\n");
        sb.append(String.format(java.util.Locale.US,
                "\nApproximate is %.2f times the best (guaranteed to be at most 2).",
                (double) r.cover.size() / best.size()));
        return sb.toString();
    }

    private static String names(List<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(CITY[ids.get(i)]);
        }
        return sb.toString();
    }
}