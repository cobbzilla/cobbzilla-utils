package org.cobbzilla.util.cache;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.system.Sleep;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class BackgroundRefreshingReference<T> extends AutoRefreshingReference<T> {

    private final AtomicBoolean updateInProgress = new AtomicBoolean(false);
    private final Refresher refresher = new Refresher();
    private final AtomicInteger errorCount = new AtomicInteger(0);

    public boolean initialize () { return true; }

    public BackgroundRefreshingReference() {
        if (initialize()) update();
    }

    @Override public void update() {
        synchronized (updateInProgress) {
            if (updateInProgress.get()) return;
            updateInProgress.set(true);
            new Thread(refresher).start();
        }
    }

    private class Refresher implements Runnable {
        @Override public void run() {
            try {
                int errCount = errorCount.get();
                if (errCount > 0) {
                    Sleep.sleep(TimeUnit.SECONDS.toMillis(1) * (long) Math.pow(2, Math.min(errCount, 6)));
                }
                set(refresh());
                errorCount.set(0);

            } catch (Exception e) {
                log.warn("error refreshing: "+e);
                errorCount.incrementAndGet();

            } finally {
                synchronized (updateInProgress) {
                    updateInProgress.set(false);
                }
            }
        }
    }
}
