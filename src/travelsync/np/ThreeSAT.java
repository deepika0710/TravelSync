package travelsync.np;

import java.util.List;

public class ThreeSAT {

    // A clause has 3 literals. Literal 3 means "variable 3 is true", literal -3 means "variable 3 is false".
    // A clause is happy if at least ONE of its literals is true.
    public static boolean clauseSatisfied(int[] clause, boolean[] value) {
        for (int lit : clause) {
            boolean v = value[Math.abs(lit)];
            if ((lit > 0 && v) || (lit < 0 && !v)) return true;
        }
        return false;
    }

    public static boolean allSatisfied(List<int[]> clauses, boolean[] value) {
        for (int[] c : clauses) {
            if (!clauseSatisfied(c, value)) return false;
        }
        return true;
    }

    // Tries every true/false combination (fine for a handful of variables).
    // Returns value[1..numVars] for a working plan, or null if the constraints conflict.
    public static boolean[] solve(int numVars, List<int[]> clauses) {
        for (int mask = 0; mask < (1 << numVars); mask++) {
            boolean[] value = new boolean[numVars + 1];
            for (int v = 1; v <= numVars; v++) {
                value[v] = (mask & (1 << (v - 1))) != 0;
            }
            if (allSatisfied(clauses, value)) return value;
        }
        return null;
    }

    public static String literalName(int lit, String[] names) {
        return (lit < 0 ? "NOT " : "") + names[Math.abs(lit) - 1];
    }

    public static String describeClause(int[] clause, String[] names) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < clause.length; i++) {
            if (i > 0) sb.append(" OR ");
            sb.append(literalName(clause[i], names));
        }
        return sb.append(")").toString();
    }
}