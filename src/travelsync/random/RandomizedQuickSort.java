package travelsync.random;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class RandomizedQuickSort<T> {

    private final Comparator<T> cmp;
    private final Random rng;
    public int comparisons = 0; // how many comparisons this run needed

    public RandomizedQuickSort(Comparator<T> cmp) {
        this.cmp = cmp;
        this.rng = new Random();
    }

    public void sort(List<T> list) {
        sort(list, 0, list.size() - 1);
    }

    private void sort(List<T> a, int lo, int hi) {
        if (lo >= hi) return;
        int p = partition(a, lo, hi);
        sort(a, lo, p - 1);
        sort(a, p + 1, hi);
    }

    // The "randomized" part: the pivot is picked at random, so no input can reliably trick the sort.
    private int partition(List<T> a, int lo, int hi) {
        int r = lo + rng.nextInt(hi - lo + 1);
        swap(a, r, hi);
        T pivot = a.get(hi);
        int i = lo;
        for (int j = lo; j < hi; j++) {
            comparisons++;
            if (cmp.compare(a.get(j), pivot) <= 0) {
                swap(a, i, j);
                i++;
            }
        }
        swap(a, i, hi);
        return i;
    }

    private void swap(List<T> a, int i, int j) {
        T tmp = a.get(i);
        a.set(i, a.get(j));
        a.set(j, tmp);
    }
}