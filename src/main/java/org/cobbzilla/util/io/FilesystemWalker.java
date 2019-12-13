package org.cobbzilla.util.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.AwaitResult;
import org.cobbzilla.util.string.StringUtil;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.Await.awaitAll;
import static org.cobbzilla.util.daemon.DaemonThreadFactory.fixedPool;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.isSymlink;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.cobbzilla.util.time.TimeUtil.parseDuration;

@Accessors(chain=true) @Slf4j
public class FilesystemWalker {

    @Getter private final List<File> dirs = new ArrayList<>();
    @Getter private final List<FilesystemVisitor> visitors = new ArrayList<>();
    @Getter @Setter private boolean includeSymlinks = true;
    @Getter @Setter private boolean visitDirs = false;
    @Getter @Setter private int threads = 5;
    @Getter @Setter private int size = 1_000_000;
    @Getter @Setter private long timeout = TimeUnit.MINUTES.toMillis(15);
    @Getter @Setter private FileFilter filter;
    @Getter @Setter private long sleepTime = TimeUnit.SECONDS.toMillis(5);

    public boolean hasFilter () { return filter != null; }

    public FilesystemWalker withDir (File dir) { dirs.add(dir); return this; }
    public FilesystemWalker withDirs (List<File> dirs) { this.dirs.addAll(dirs); return this; }
    public FilesystemWalker withDirs (File[] dirs) { this.dirs.addAll(Arrays.asList(dirs)); return this; }
    public FilesystemWalker withVisitor (FilesystemVisitor visitor) { visitors.add(visitor); return this; }
    public FilesystemWalker withTimeoutDuration (String duration) { setTimeout(parseDuration(duration)); return this; }

    @Getter(lazy=true) private final ExecutorService pool = fixedPool(getThreads());
    @Getter(lazy=true) private final List<Future<?>> futures = new ArrayList<>(getSize());

    public AwaitResult walk() {
        for (File dir : dirs) fileJob(dir);

        // wait for number of futures to stop increasing
        do {
            final int lastNumFutures = numFutures();
            awaitFutures();
            if (numFutures() == lastNumFutures) break;
            sleep(getSleepTime());
        } while (true);
        return awaitFutures();
    }

    private AwaitResult awaitFutures() {
        final AwaitResult result = awaitAll(getFutures(), getTimeout());
        if (!result.allSucceeded()) log.warn(StringUtil.toString(result.getFailures().values(), "\n---------"));
        return result;
    }

    private int numFutures () {
        final List<Future<?>> futures = getFutures();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (futures) { return futures.size(); }
    }

    private boolean fileJob(File f) {
        final List<Future<?>> futures = getFutures();
        final Future<?> future = getPool().submit(new FsWalker(f));
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (futures) { return futures.add(future); }
    }

    @AllArgsConstructor
    private class FsWalker implements Runnable {
        private final File file;

        @Override public void run() {
            if (isSymlink(file) && !includeSymlinks) return;

            if (file.isFile()) {
                // visit the file
                visit();

            } else if (file.isDirectory()) {
                // should we visit directory entries?
                if (visitDirs) visit();

                // should we filter directory entries?
                final File[] files = hasFilter() ? file.listFiles(filter) : file.listFiles();

                // walk each entry in the directory
                if (files != null) for (File f : files) fileJob(f);

            } else {
                log.warn("unexpected file: neither file nor directory, skipping: "+abs(file));
            }

        }

        private void visit() { for (FilesystemVisitor visitor : getVisitors()) visitor.visit(file); }
    }
}
