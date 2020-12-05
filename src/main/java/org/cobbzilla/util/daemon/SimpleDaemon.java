package org.cobbzilla.util.daemon;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.system.Sleep.sleep;

@Slf4j
public abstract class SimpleDaemon implements Runnable {

    public static final DateTimeFormatter DFORMAT = DateTimeFormat.forPattern("yyyy-MMM-dd HH:mm:ss");

    public SimpleDaemon () { this.name = getClass().getSimpleName(); }

    public SimpleDaemon (String name) { this.name = name; }

    @Getter private final String name;
    @Getter private long lastProcessTime = 0;

    private volatile Thread mainThread = null;
    private final Object lock = new Object();
    private volatile boolean isDone = false;

    /** Called right after daemon has started */
    public void onStart () {}

    /** Called right before daemon is about to exit */
    public void onStop () {}

    public boolean start() {
        log.info(getName()+": Starting daemon");
        synchronized (lock) {
            if (mainThread != null) {
                log.info(getName()+": daemon is already running, not starting it again");
                return false;
            }
            mainThread = new Thread(this);
            mainThread.setName(getName());
        }
        mainThread.setDaemon(true);
        mainThread.start();
        return true;
    }

    public boolean startOrInterrupt() {
        if (start()) return true;
        if (canInterruptSleep()) interrupt();
        return false;
    }

    private boolean alreadyStopped() {
        if (mainThread == null) {
            log.warn(getName()+": daemon is already stopped");
            return true;
        }
        return false;
    }

    public void stop() {
        if (alreadyStopped()) return;
        isDone = true;
        mainThread.interrupt();
        // Let's leave it at that, this thread is a daemon anyway.
    }

    public void interrupt() {
        if (alreadyStopped()) return;
        mainThread.interrupt();
    }

    /**
     * @deprecated USE WITH CAUTION -- calls Thread.stop() !!
     */
    @Deprecated
    private void kill() {
        if (alreadyStopped()) return;
        isDone = true;
        mainThread.stop();
    }

    /**
     * Tries to stop the daemon.  If it doesn't stop within "wait" millis,
     * it gets killed.
     */
    public void stopWithPossibleKill(long wait) {
        stop();
        long start = now();
        while (getIsAlive()
                && (now() - start < wait)) {
            wait(25, "stopWithPossibleKill");
        }
        if (getIsAlive()) {
            kill();
        }
    }

    protected void init() throws Exception {}

    public void run() {
        onStart();
        long delay = getStartupDelay();
        if (delay > 0) {
            log.debug(getName()+": Delaying daemon startup for " + delay + "ms...");
            if (!wait(delay, "run[startup-delay]")) {
                if (!canInterruptSleep()) return;
            }
        }
        log.debug(getName()+": Daemon thread now running");

        try {
            log.debug(getName()+": Daemon thread invoking init");
            init();

            while (!isDone) {
                if (log.isTraceEnabled()) log.trace(getName()+": Daemon thread invoking process");
                try {
                    process();
                    lastProcessTime = now();
                } catch (Exception e) {
                    processException(e);
                    continue;
                }
                if (isDone) return;
                if (!wait(getSleepTime(), "run[post-processing]")) {
                    if (canInterruptSleep()) continue;
                    return;
                }
            }
        } catch (Exception e) {
            log.error(getName()+": Error in daemon, exiting: " + e, e);

        } finally {
            cleanup();
            try {
                onStop();
            } catch (Exception e) {
                log.error(getName()+": Error in onStop, exiting and ignoring error: " + e, e);
            }
        }
    }

    public void processException(Exception e) throws Exception { throw e; }

    protected boolean wait(long delay, String reason) {
        try {
            sleep(delay, reason);
            return true;
        } catch (RuntimeException e) {
            if (isDone) {
                log.info(getName()+": sleep("+delay+") interrupted but daemon is done");
            } else {
                if (canInterruptSleep()) {
                    log.info(getName() + ": sleep(" + delay + ") interrupted and canInterruptSleep() returned true, continuing...");
                } else {
                    log.error(getName() + ": sleep(" + delay + ") interrupted, exiting: " + e);
                }
            }
            return false;
        }
    }

    protected boolean canInterruptSleep() { return false; }

    protected long getStartupDelay() { return 0; }

    protected abstract long getSleepTime();

    protected abstract void process();

    public boolean getIsDone() { return isDone; }

    public boolean getIsAlive() {
        try {
            return mainThread != null && mainThread.isAlive();
        } catch (NullPointerException npe) {
            return false;
        }
    }

    private void cleanup() {
        mainThread = null;
        isDone = true;
    }

    public String getStatus() {
        return "isDone=" + getIsDone()
                + "\nlastProcessTime=" + DFORMAT.print(lastProcessTime)
                + "\nsleepTime=" + getSleepTime()+"ms";
    }
}
