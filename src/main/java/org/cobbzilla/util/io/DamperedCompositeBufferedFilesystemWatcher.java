package org.cobbzilla.util.io;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.system.Sleep.nap;

@Slf4j
public abstract class DamperedCompositeBufferedFilesystemWatcher extends CompositeBufferedFilesystemWatcher {

    private final AtomicLong damper = new AtomicLong(0);
    private final AtomicReference<Thread> damperThread = new AtomicReference<>();
    private final AtomicReference<List<WatchEvent<?>>> buffer = new AtomicReference<>();

    protected void init(long damperDuration, int maxEvents) {

        this.damper.set(damperDuration);
        this.buffer.set(new ArrayList<WatchEvent<?>>(maxEvents*10));

        this.damperThread.set(new Thread(new Runnable() {
            @Override public void run() {
                log.debug(status()+" starting, sleeping for a while until there is some activity");
                //noinspection InfiniteLoopStatement
                while (true) {
                    if (!nap(TimeUnit.HOURS.toMillis(4), "waiting for filesystem watcher trigger to fire")) {
                        // we were interrupted. sleep for the damper time
                        while (!nap(damper.get())) {
                            // there was more activity, go back to sleep
                            log.debug(status()+" more activity while napping for damper, trying again");
                        }
                        // we successfully napped without being interrupted! fire the big trigger
                        log.debug(status()+": napped successfully, calling fire");
                        List<WatchEvent<?>> events;
                        synchronized (buffer) {
                            events = new ArrayList<>(buffer.get());
                            buffer.get().clear();
                        }
                        uber_fire(events);
                    }
                    log.debug(status()+" just fired, going back to sleep for a while until there is some more activity");
                }
            }
        }));
        damperThread.get().setDaemon(true);
        damperThread.get().start();
    }

    protected String status() { synchronized (buffer) { return "[" + buffer.get().size() + " events]"; } }

    /**
     * Called when the thing finally really fires.
     * @param events
     */
    public abstract void uber_fire(List<WatchEvent<?>> events);

    @Override public void fire(List<WatchEvent<?>> events) {
        log.debug(status()+": fire adding "+events.size()+" events...");
        synchronized (buffer) {
            buffer.get().addAll(events);
        }
        synchronized (damperThread.get()) {
            damperThread.get().interrupt();
        }
    }

    public DamperedCompositeBufferedFilesystemWatcher(long timeout, int maxEvents, long damper) {
        super(timeout, maxEvents);
        init(damper, maxEvents);
    }

    public DamperedCompositeBufferedFilesystemWatcher(long timeout, int maxEvents, File[] paths, long damper) {
        super(timeout, maxEvents, paths);
        init(damper, maxEvents);
    }

    public DamperedCompositeBufferedFilesystemWatcher(long timeout, int maxEvents, String[] paths, long damper) {
        super(timeout, maxEvents, paths);
        init(damper, maxEvents);
    }

    public DamperedCompositeBufferedFilesystemWatcher(long timeout, int maxEvents, Path[] paths, long damper) {
        super(timeout, maxEvents, paths);
        init(damper, maxEvents);
    }

    public DamperedCompositeBufferedFilesystemWatcher(long timeout, int maxEvents, Collection things, long damper) {
        super(timeout, maxEvents, things);
        init(damper, maxEvents);
    }

}
