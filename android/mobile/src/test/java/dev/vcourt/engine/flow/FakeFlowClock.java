package dev.vcourt.engine.flow;

/** A clock that moves only when slept on, so timeouts pass at once. */
final class FakeFlowClock implements FlowClock {
    double now;

    @Override
    public double now() {
        return now;
    }

    @Override
    public void sleep(double seconds) {
        now += seconds;
    }
}
