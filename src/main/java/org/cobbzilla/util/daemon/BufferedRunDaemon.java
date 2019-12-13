package org.cobbzilla.util.daemon;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.ZillaRuntime.background;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.system.Sleep.nap;
import static org.cobbzilla.util.time.TimeUtil.formatDuration;

@AllArgsConstructor @Slf4j
public class BufferedRunDaemon implements Runnable {

    public static final long IDLE_SYNC_INTERVAL = HOURS.toMillis(1);
    public static final long MIN_SYNC_WAIT      = SECONDS.toMillis(10);

    private final String logPrefix;
    private final Runnable action;

    private final AtomicReference<Thread> daemonThread = new AtomicReference<>();
    private final AtomicLong lastRun = new AtomicLong(0);
    private final AtomicLong lastRunRequested = new AtomicLong(0);

    private final AtomicBoolean done = new AtomicBoolean(false);

    protected long getIdleSyncInterval() { return IDLE_SYNC_INTERVAL; }
    protected long getMinSyncWait     () { return MIN_SYNC_WAIT; }

    public void start () { daemonThread.set(background(this)); }

    protected void interrupt() { if (daemonThread.get() != null) daemonThread.get().interrupt(); }

    public void poke () { lastRunRequested.set(now()); interrupt(); }
    public void done () { done.set(true); interrupt(); }

    @Override public void run () {
        long napTime;

        //noinspection InfiniteLoopStatement
        while (true) {
            napTime = getIdleSyncInterval();
            log.info(logPrefix+": sleep for "+formatDuration(napTime)+" awaiting activity");
            if (!nap(napTime, logPrefix+" napping for "+formatDuration(napTime)+" awaiting activity")) {
                log.info(logPrefix + " interrupted during initial pause, continuing");
            } else {
                boolean shouldDoIdleSleep = lastRunRequested.get() == 0;
                if (shouldDoIdleSleep) {
                    shouldDoIdleSleep = lastRunRequested.get() == 0;
                    while (shouldDoIdleSleep && lastRun.get() > 0 && now() - lastRun.get() < getIdleSyncInterval()) {
                        log.info(logPrefix + " napping for " + formatDuration(napTime) + " due to no activity");
                        if (!nap(napTime, logPrefix + " idle loop sleep")) {
                            log.info(logPrefix + " nap was interrupted, breaking out");
                            break;
                        }
                        shouldDoIdleSleep = lastRunRequested.get() == 0;
                    }
                }
            }

            final long minSyncWait = getMinSyncWait();
            while (lastRunRequested.get() > 0 && now() - lastRunRequested.get() < minSyncWait) {
                napTime = minSyncWait / 4;
                log.info(logPrefix+" napping for "+formatDuration(napTime)+", waiting for at least "+formatDuration(minSyncWait)+" of no activity before starting sync");
                nap(napTime, logPrefix + " waiting for inactivity");
            }

            try {
                action.run();
            } catch (Exception e) {
                log.error(logPrefix+" sync: " + e, e);
            } finally {
                lastRun.set(now());
                lastRunRequested.set(0);
            }
        }
    }
}
