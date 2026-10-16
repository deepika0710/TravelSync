package travelsync.random;

import java.math.BigInteger;

public class MillerRabin {

    // These 12 witnesses give a guaranteed-correct answer for every number that fits in a long.
    private static final long[] WITNESSES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};

    public static boolean isPrime(long n) {
        if (n < 2) return false;
        if (n < 4) return true;
        if ((n & 1) == 0) return false;
        long d = n - 1;
        int r = 0;
        while ((d & 1) == 0) {
            d >>= 1;
            r++;
        }
        for (long a : WITNESSES) {
            if (a >= n) break;   // a witness must be smaller than the number being tested
            if (!passes(a, d, r, n)) return false;
        }
        return true;
    }

    // Same test, but returns a step-by-step explanation for the dashboard.
    public static String explain(long n) {
        StringBuilder sb = new StringBuilder();
        sb.append("Number: ").append(n).append("\n");
        if (n < 2) return sb.append("Result: NOT prime (primes start at 2)").toString();
        if (n < 4) return sb.append("Result: PRIME").toString();
        if ((n & 1) == 0) return sb.append("Result: COMPOSITE (even number)").toString();
        long d = n - 1;
        int r = 0;
        while ((d & 1) == 0) {
            d >>= 1;
            r++;
        }
        sb.append("Write n - 1 = 2^").append(r).append(" x ").append(d).append("\n");
        int tested = 0;
        for (long a : WITNESSES) {
            if (a >= n) break;   // a witness must be smaller than the number being tested
            boolean ok = passes(a, d, r, n);
            tested++;
            sb.append("Witness ").append(a).append(": ").append(ok ? "passes" : "FAILS").append("\n");
            if (!ok) {
                return sb.append("Result: COMPOSITE (witness ").append(a).append(" proves it)").toString();
            }
        }
        return sb.append("Result: PRIME (all ").append(tested).append(" witnesses passed)").toString();
    }

    // One round: a prime number cannot fail this, a composite number usually does.
    private static boolean passes(long a, long d, int r, long n) {
        long x = powMod(a, d, n);
        if (x == 1 || x == n - 1) return true;
        for (int i = 1; i < r; i++) {
            x = mulMod(x, x, n);
            if (x == n - 1) return true;
        }
        return false;
    }

    // a * b mod m without overflow (BigInteger handles the huge intermediate number)
    private static long mulMod(long a, long b, long m) {
        return BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)).mod(BigInteger.valueOf(m)).longValue();
    }

    // base^exp mod m using repeated squaring
    private static long powMod(long base, long exp, long m) {
        long result = 1;
        base %= m;
        while (exp > 0) {
            if ((exp & 1) == 1) result = mulMod(result, base, m);
            base = mulMod(base, base, m);
            exp >>= 1;
        }
        return result;
    }
}