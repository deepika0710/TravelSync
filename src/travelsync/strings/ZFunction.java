package travelsync.strings;

import java.util.ArrayList;
import java.util.List;

public class ZFunction {

    // z[i] = length of the longest match between the string start and the substring starting at i
    public static int[] compute(String s) {
        int n = s.length();
        int[] z = new int[n];
        int l = 0, r = 0;
        for (int i = 1; i < n; i++) {
            if (i < r) {
                z[i] = Math.min(r - i, z[i - l]);
            }
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) {
                z[i]++;
            }
            if (i + z[i] > r) {
                l = i;
                r = i + z[i];
            }
        }
        return z;
    }

    // pattern search using Z-function
    public static List<Integer> search(String text, String pattern) {
        List<Integer> result = new ArrayList<>();
        if (pattern.isEmpty()) return result;
        String combined = pattern + "\u0001" + text;
        int[] z = compute(combined);
        for (int i = pattern.length() + 1; i < combined.length(); i++) {
            if (z[i] == pattern.length()) {
                result.add(i - pattern.length() - 1);
            }
        }
        return result;
    }

    // the longest beginning of the text that shows up again later (a repeated phrase)
    public static String longestRepeatedPrefix(String s) {
        int[] z = compute(s);
        int best = 0;
        for (int i = 1; i < z.length; i++) {
            best = Math.max(best, z[i]);
        }
        return s.substring(0, best);
    }
}