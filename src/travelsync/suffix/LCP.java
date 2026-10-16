package travelsync.suffix;

public class LCP {

    // Kasai's algorithm. lcp[k] = number of starting letters shared by suffixes sa[k] and sa[k+1]
    public static int[] kasai(String s, int[] sa) {
        int n = s.length();
        if (n <= 1) return new int[0];
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) rank[sa[i]] = i;

        int[] lcp = new int[n - 1];
        int h = 0;
        for (int i = 0; i < n; i++) {
            if (h > 0) h--;
            if (rank[i] == 0) continue;
            int j = sa[rank[i] - 1];
            while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
            lcp[rank[i] - 1] = h;
        }
        return lcp;
    }
}