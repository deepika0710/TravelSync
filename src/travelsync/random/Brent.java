package travelsync.random;

public class Brent {

    // Brent's theorem: with p processors, the time is at most  work / p + depth
    public static double bound(double work, double depth, int p) {
        return work / p + depth;
    }

    // depth of a "halving" tree over n items = ceil(log2 n)
    public static int log2Ceil(long n) {
        return 64 - Long.numberOfLeadingZeros(n - 1);
    }
}