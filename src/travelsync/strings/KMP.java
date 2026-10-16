package travelsync.strings;

import java.util.ArrayList;
import java.util.List;

public class KMP {

    // lps[i] = length of the longest prefix of the pattern that is also a suffix ending at i
    public static int[] buildLPS(String p) {
        int[] lps = new int[p.length()];
        int len = 0;
        int i = 1;
        while (i < p.length()) {
            if (p.charAt(i) == p.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else if (len > 0) {
                len = lps[len - 1];
            } else {
                lps[i] = 0;
                i++;
            }
        }
        return lps;
    }

    // returns all start positions where pattern occurs in text
    public static List<Integer> search(String text, String pattern) {
        List<Integer> result = new ArrayList<>();
        if (pattern.isEmpty()) return result;
        int[] lps = buildLPS(pattern);
        int j = 0;
        for (int i = 0; i < text.length(); i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = lps[j - 1];
            }
            if (text.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            if (j == pattern.length()) {
                result.add(i - j + 1);
                j = lps[j - 1];
            }
        }
        return result;
    }

    public static List<Integer> searchIgnoreCase(String text, String pattern) {
        return search(text.toLowerCase(), pattern.toLowerCase());
    }
}