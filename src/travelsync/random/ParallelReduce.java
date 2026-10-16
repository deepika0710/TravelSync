package travelsync.random;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.LongBinaryOperator;

public class ParallelReduce {

    private static final int THRESHOLD = 10_000; // below this size, just add up directly

    // Split the data in half, let two workers handle one half each, then combine the two answers.
    @SuppressWarnings("serial")
    private static class ReduceTask extends RecursiveTask<Long> {
        private final long[] data;
        private final int lo, hi;
        private final long identity;
        private final LongBinaryOperator op;

        ReduceTask(long[] data, int lo, int hi, long identity, LongBinaryOperator op) {
            this.data = data;
            this.lo = lo;
            this.hi = hi;
            this.identity = identity;
            this.op = op;
        }

        @Override
        protected Long compute() {
            if (hi - lo <= THRESHOLD) {
                long result = identity;
                for (int i = lo; i < hi; i++) result = op.applyAsLong(result, data[i]);
                return result;
            }
            int mid = (lo + hi) >>> 1;
            ReduceTask left = new ReduceTask(data, lo, mid, identity, op);
            ReduceTask right = new ReduceTask(data, mid, hi, identity, op);
            left.fork();                          // hand the left half to another worker
            long r = right.compute();             // do the right half ourselves
            return op.applyAsLong(left.join(), r);
        }
    }

    public static long reduce(long[] data, long identity, LongBinaryOperator op) {
        return ForkJoinPool.commonPool().invoke(new ReduceTask(data, 0, data.length, identity, op));
    }

    public static long sum(long[] data) {
        return reduce(data, 0, Long::sum);
    }

    public static long max(long[] data) {
        return reduce(data, Long.MIN_VALUE, Math::max);
    }

    public static long sequentialSum(long[] data) {
        long s = 0;
        for (long v : data) s += v;
        return s;
    }
}