package travelsync.suffix;

import java.util.List;
import travelsync.model.Destination;

public class CommonDescriptions {

    // Longest piece of text shared by two descriptions (suffix array + LCP)
    public static String longestCommonSubstring(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        String t = a + "\u0001" + b;
        int[] sa = SuffixArray.build(t);
        int[] lcp = LCP.kasai(t, sa);

        int best = 0, bestPos = 0;
        for (int k = 0; k < lcp.length; k++) {
            boolean firstInA = sa[k] < a.length();
            boolean secondInA = sa[k + 1] < a.length();
            if (firstInA != secondInA && lcp[k] > best) {
                best = lcp[k];
                bestPos = sa[k];
            }
        }
        return t.substring(bestPos, bestPos + best);
    }

    // Finds the two destinations whose descriptions share the longest text
    public static String mostSimilarPair(List<Destination> list) {
        String bestText = "";
        String bestPair = "none";
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                String common = longestCommonSubstring(
                        list.get(i).getDescription(), list.get(j).getDescription());
                if (common.length() > bestText.length()) {
                    bestText = common;
                    bestPair = list.get(i).getName() + " & " + list.get(j).getName();
                }
            }
        }
        return bestPair + " share: \"" + bestText + "\"";
    }
}