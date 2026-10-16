package travelsync.suffix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SuffixArray {

    // Builds the suffix array using SA-IS (fast, linear time)
    public static int[] build(String text) {
        int n = text.length();
        int[] s = new int[n];
        int upper = 0;
        for (int i = 0; i < n; i++) {
            s[i] = text.charAt(i);
            upper = Math.max(upper, s[i]);
        }
        return saIs(s, upper);
    }

    // Slow but simple version, used only to double-check SA-IS
    public static int[] buildNaive(String text) {
        int n = text.length();
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> text.substring(a).compareTo(text.substring(b)));
        int[] sa = new int[n];
        for (int i = 0; i < n; i++) sa[i] = idx[i];
        return sa;
    }

    private static int[] saIs(int[] s, int upper) {
        int n = s.length;
        if (n == 0) return new int[0];
        if (n == 1) return new int[] { 0 };
        if (n == 2) {
            return s[0] < s[1] ? new int[] { 0, 1 } : new int[] { 1, 0 };
        }

        int[] sa = new int[n];
        boolean[] ls = new boolean[n]; // true = S-type, false = L-type
        for (int i = n - 2; i >= 0; i--) {
            ls[i] = (s[i] == s[i + 1]) ? ls[i + 1] : (s[i] < s[i + 1]);
        }

        int[] sumL = new int[upper + 1];
        int[] sumS = new int[upper + 1];
        for (int i = 0; i < n; i++) {
            if (!ls[i]) sumS[s[i]]++;
            else sumL[s[i] + 1]++;
        }
        for (int i = 0; i <= upper; i++) {
            sumS[i] += sumL[i];
            if (i < upper) sumL[i + 1] += sumS[i];
        }

        int[] lmsMap = new int[n + 1];
        Arrays.fill(lmsMap, -1);
        int m = 0;
        for (int i = 1; i < n; i++) {
            if (!ls[i - 1] && ls[i]) lmsMap[i] = m++;
        }
        int[] lms = new int[m];
        int idx = 0;
        for (int i = 1; i < n; i++) {
            if (!ls[i - 1] && ls[i]) lms[idx++] = i;
        }

        induce(s, sa, ls, sumS, sumL, lms, upper);

        if (m > 0) {
            int[] sortedLms = new int[m];
            int k = 0;
            for (int v : sa) {
                if (lmsMap[v] != -1) sortedLms[k++] = v;
            }
            int[] recS = new int[m];
            int recUpper = 0;
            recS[lmsMap[sortedLms[0]]] = 0;
            for (int i = 1; i < m; i++) {
                int l = sortedLms[i - 1];
                int r = sortedLms[i];
                int endL = (lmsMap[l] + 1 < m) ? lms[lmsMap[l] + 1] : n;
                int endR = (lmsMap[r] + 1 < m) ? lms[lmsMap[r] + 1] : n;
                boolean same = true;
                if (endL - l != endR - r) {
                    same = false;
                } else {
                    while (l < endL) {
                        if (s[l] != s[r]) break;
                        l++;
                        r++;
                    }
                    if (l == n || s[l] != s[r]) same = false;
                }
                if (!same) recUpper++;
                recS[lmsMap[sortedLms[i]]] = recUpper;
            }
            int[] recSa = saIs(recS, recUpper);
            for (int i = 0; i < m; i++) {
                sortedLms[i] = lms[recSa[i]];
            }
            induce(s, sa, ls, sumS, sumL, sortedLms, upper);
        }
        return sa;
    }

    private static void induce(int[] s, int[] sa, boolean[] ls, int[] sumS, int[] sumL,
                               int[] lmsList, int upper) {
        int n = s.length;
        Arrays.fill(sa, -1);
        int[] buf = new int[upper + 1];

        System.arraycopy(sumS, 0, buf, 0, upper + 1);
        for (int d : lmsList) {
            if (d == n) continue;
            sa[buf[s[d]]++] = d;
        }

        System.arraycopy(sumL, 0, buf, 0, upper + 1);
        sa[buf[s[n - 1]]++] = n - 1;
        for (int i = 0; i < n; i++) {
            int v = sa[i];
            if (v >= 1 && !ls[v - 1]) sa[buf[s[v - 1]]++] = v - 1;
        }

        System.arraycopy(sumL, 0, buf, 0, upper + 1);
        for (int i = n - 1; i >= 0; i--) {
            int v = sa[i];
            if (v >= 1 && ls[v - 1]) sa[--buf[s[v - 1] + 1]] = v - 1;
        }
    }

    // Finds every position where pattern occurs in text (binary search on the suffix array)
    public static List<Integer> search(String text, int[] sa, String pattern) {
        List<Integer> result = new ArrayList<>();
        if (pattern.isEmpty()) return result;

        int lo = 0, hi = sa.length;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (cmp(text, sa[mid], pattern) < 0) lo = mid + 1;
            else hi = mid;
        }
        int start = lo;

        hi = sa.length;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (cmp(text, sa[mid], pattern) <= 0) lo = mid + 1;
            else hi = mid;
        }
        int end = lo;

        for (int i = start; i < end; i++) result.add(sa[i]);
        return result;
    }

    // compares the suffix at pos with the pattern (only the first pattern.length() letters)
    private static int cmp(String text, int pos, String p) {
        int n = Math.min(p.length(), text.length() - pos);
        for (int i = 0; i < n; i++) {
            int diff = text.charAt(pos + i) - p.charAt(i);
            if (diff != 0) return diff;
        }
        return (text.length() - pos < p.length()) ? -1 : 0;
    }
}