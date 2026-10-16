package travelsync;

import java.util.Arrays;
import travelsync.model.*;
import travelsync.suffix.*;

public class TestM2 {
    public static void main(String[] args) {
        String s = "banana";
        int[] sa = SuffixArray.build(s);
        System.out.println("Suffix array of banana: " + Arrays.toString(sa));
        System.out.println("LCP (Kasai): " + Arrays.toString(LCP.kasai(s, sa)));

        String[] tests = { "banana", "mississippi", "abracadabra", "aaaaaa", "travelsync is fun" };
        boolean allMatch = true;
        for (String t : tests) {
            if (!Arrays.equals(SuffixArray.build(t), SuffixArray.buildNaive(t))) allMatch = false;
        }
        System.out.println("SA-IS matches naive on all tests: " + allMatch);

        System.out.println("Goa & Bali share: \""
                + CommonDescriptions.longestCommonSubstring(
                        SampleData.destinations().get(0).getDescription(),
                        SampleData.destinations().get(2).getDescription()) + "\"");
        System.out.println("Most similar pair: "
                + CommonDescriptions.mostSimilarPair(SampleData.destinations()));

        SuffixAutomaton auto = new SuffixAutomaton("banana");
        System.out.println("Automaton contains 'nan': " + auto.contains("nan"));
        System.out.println("Automaton contains 'nab': " + auto.contains("nab"));
        System.out.println("Distinct substrings of banana: " + auto.countDistinctSubstrings());

        TravelIndex index = new TravelIndex(SampleData.destinations(), SampleData.hotels());
        System.out.println("Places mentioning 'sea': " + index.search("sea"));
    }
}