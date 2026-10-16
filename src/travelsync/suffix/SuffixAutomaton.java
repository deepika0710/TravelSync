package travelsync.suffix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuffixAutomaton {

    private final List<Map<Character, Integer>> next = new ArrayList<>();
    private final List<Integer> link = new ArrayList<>();
    private final List<Integer> len = new ArrayList<>();
    private int last;

    public SuffixAutomaton(String text) {
        newState(0, -1); // state 0 = empty string
        last = 0;
        for (char c : text.toCharArray()) extend(c);
    }

    private int newState(int length, int suffixLink) {
        next.add(new HashMap<>());
        len.add(length);
        link.add(suffixLink);
        return len.size() - 1;
    }

    private void extend(char c) {
        int cur = newState(len.get(last) + 1, 0);
        int p = last;
        while (p != -1 && !next.get(p).containsKey(c)) {
            next.get(p).put(c, cur);
            p = link.get(p);
        }
        if (p == -1) {
            link.set(cur, 0);
        } else {
            int q = next.get(p).get(c);
            if (len.get(p) + 1 == len.get(q)) {
                link.set(cur, q);
            } else {
                int clone = newState(len.get(p) + 1, link.get(q));
                next.get(clone).putAll(next.get(q));
                while (p != -1 && next.get(p).getOrDefault(c, -1) == q) {
                    next.get(p).put(c, clone);
                    p = link.get(p);
                }
                link.set(q, clone);
                link.set(cur, clone);
            }
        }
        last = cur;
    }

    // does the text contain this word/phrase anywhere?
    public boolean contains(String pattern) {
        int state = 0;
        for (char c : pattern.toCharArray()) {
            Integer nx = next.get(state).get(c);
            if (nx == null) return false;
            state = nx;
        }
        return true;
    }

    // number of different substrings in the text
    public long countDistinctSubstrings() {
        long total = 0;
        for (int v = 1; v < len.size(); v++) {
            total += len.get(v) - len.get(link.get(v));
        }
        return total;
    }
}