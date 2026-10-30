package dev.vcourt.engine.driver;

import java.util.ArrayList;
import java.util.List;

/** A clock that moves only when slept on; it can be interrupted once at a given sleep. */
final class FakeClock implements DriverSession.Clock {
    final List<Long> sleeps = new ArrayList<>();
    long now = 1_000;
    int interruptAtSleep = -1;

    @Override
    public long millis() {
        return now;
    }

    @Override
    public void sleep(long millis) throws InterruptedException {
        if (sleeps.size() == interruptAtSleep) {
            interruptAtSleep = -1;
            throw new InterruptedException("scripted");
        }
        sleeps.add(millis);
        now += millis;
    }
}
