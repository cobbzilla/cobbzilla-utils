package org.cobbzilla.util.io;

import lombok.Getter;
import lombok.ToString;
import org.cobbzilla.util.collection.InspectCollection;
import org.cobbzilla.util.system.Sleep;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.daemon.ZillaRuntime.terminate;
import static org.cobbzilla.util.io.FileUtil.abs;

/**
 * Sometimes you just want to know that something changed, and you don't really care what.
 * Extend this class and override the "fire" method. You will receive one callback when
 * your timeout elapses, or if the buffer of events exceeds maxEvents.
 */
@ToString(callSuper=true, of={"timeout", "maxEvents"})
public abstract class BufferedFilesystemWatcher extends FilesystemWatcher implements Closeable {

    @Getter private final long timeout;
    @Getter private final int maxEvents;

    private final Thread monitor;
    private long lastFlush;
    private final Queue<WatchEvent<?>> buffer = new ConcurrentLinkedQueue<>();
    private BfsMonitor bfsMonitor;

    /**
     * Called when some changes have occurred.
     * This will be called if the number of events exceeds maxEvents, or if
     * timeout milliseconds have elapsed since the last time it was called (any
     * at least one event has occurred)
     * @param events A collection of events.
     */
    protected abstract void fire(List<WatchEvent<?>> events);

    public BufferedFilesystemWatcher(Path path, long timeout, int maxEvents) {
        super(path);
        bfsMonitor = new BfsMonitor();
        monitor = new Thread(bfsMonitor, "bfs-monitor("+abs(path)+")");
        monitor.setDaemon(true);
        monitor.start();
        this.timeout = timeout;
        this.maxEvents = maxEvents;
    }

    public BufferedFilesystemWatcher(File path, long timeout, int maxEvents) {
        this(path.toPath(), timeout, maxEvents);
    }

    @Override public void close() throws IOException {
        if (monitor != null) {
            bfsMonitor.alive = false;
            terminate(monitor, 2000);
        }
        super.close();
    }

    private boolean beenTooLong() { return now() - lastFlush > timeout; }
    private boolean bufferTooBig() { return InspectCollection.isLargerThan(buffer, maxEvents); }

    private boolean shouldFlush() { return bufferTooBig() || (!buffer.isEmpty() && beenTooLong()); }

    @Override protected void handleEvent(WatchEvent<?> event) { buffer.add(event); }

    private class BfsMonitor implements Runnable {
        public volatile boolean alive = false;
        @Override public void run() {
            alive = true;
            while (alive) {
                Sleep.sleep(timeout / 10);
                if (shouldFlush()) flush();
            }
        }
    }

    private synchronized void flush() {
        // sanity check that we have not flushed recently
        if (!shouldFlush()) return;

        // nothing to flush?
        if (buffer.isEmpty()) return;

        final List<WatchEvent<?>> events = new ArrayList<>(buffer.size());
        while (!buffer.isEmpty()) {
            events.add(buffer.poll());
            if (events.size() > maxEvents) {
                fire(events);
                events.clear();
            }
        }
        if (!events.isEmpty()) fire(events);
        lastFlush = now();
    }

}
