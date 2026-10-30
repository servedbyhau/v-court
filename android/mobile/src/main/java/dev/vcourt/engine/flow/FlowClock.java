package dev.vcourt.engine.flow;

/** Monotonic seconds and sleeping, as the flow waits by them; tests replace them. */
public interface FlowClock {
    double now();

    void sleep(double seconds) throws InterruptedException;

    /** The system's monotonic clock. */
    FlowClock SYSTEM = new FlowClock() {
        @Override
        public double now() {
            return System.nanoTime() / 1e9;
        }

        @Override
        public void sleep(double seconds) throws InterruptedException {
            long millis = (long) (seconds * 1000);
            int nanos = (int) Math.round((seconds * 1000 - millis) * 1_000_000);
            if (millis > 0 || nanos > 0) {
                Thread.sleep(millis, Math.min(nanos, 999_999));
            }
        }
    };
}
