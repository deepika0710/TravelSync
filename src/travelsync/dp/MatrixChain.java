package travelsync.dp;

public class MatrixChain {

    public static class Result {
        public long cost;
        public String order;
    }

    // dims: matrix i has size dims[i] x dims[i+1]
    public static Result solve(int[] dims) {
        int n = dims.length - 1; // number of matrices
        long[][] m = new long[n][n];
        int[][] split = new int[n][n];

        for (int len = 2; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                m[i][j] = Long.MAX_VALUE;
                for (int k = i; k < j; k++) {
                    long cost = m[i][k] + m[k + 1][j]
                            + (long) dims[i] * dims[k + 1] * dims[j + 1];
                    if (cost < m[i][j]) {
                        m[i][j] = cost;
                        split[i][j] = k;
                    }
                }
            }
        }

        Result r = new Result();
        r.cost = m[0][n - 1];
        r.order = paren(split, 0, n - 1);
        return r;
    }

    private static String paren(int[][] split, int i, int j) {
        if (i == j) return "A" + (i + 1);
        int k = split[i][j];
        return "(" + paren(split, i, k) + paren(split, k + 1, j) + ")";
    }
}