package org.cobbzilla.util.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.getDefaultTempDir;
import static org.cobbzilla.util.system.CommandShell.chmod;
import static org.cobbzilla.util.system.OsType.isWindows;
import static org.cobbzilla.util.system.Sleep.sleep;

/**
 * A directory that implements Closeable. Use lombok @Cleanup to nuke it when it goes out of scope.
 */
@Slf4j
public class TempDir extends File implements Closeable {

    @AllArgsConstructor
    private static class FileKillOrder implements Comparable<FileKillOrder> {
        @Getter @Setter private File file;
        @Getter @Setter private long killTime;
        @Override public int compareTo(FileKillOrder k) {
            if (killTime > k.getKillTime()) return 1;
            if (killTime == k.getKillTime()) return 0;
            return -1;
        }
        public boolean shouldKill() { return killTime != QT_NO_DELETE && now() > killTime; }
    }

    private static class QuickTempReaper implements Runnable {
        private final SortedSet<FileKillOrder> temps = new ConcurrentSkipListSet<>();
        public File add (File t) { return add(t, now() + TimeUnit.MINUTES.toMillis(5)); }
        public File add (File t, long killTime) {
            synchronized (temps) {
                temps.add(new FileKillOrder(t, killTime));
                return t;
            }
        }
        @Override public void run() {
            while (true) {
                sleep(10_000);
                synchronized (temps) {
                    while (!temps.isEmpty() && temps.first().shouldKill()) {
                        if (!temps.first().getFile().delete()) {
                            log.warn("QuickTempReaper.run: couldn't delete " + abs(temps.first().getFile()));
                        }
                        temps.remove(temps.first());
                    }
                }
            }
        }
        public QuickTempReaper start () {
            daemon(this);
            return this;
        }
    }

    private static QuickTempReaper qtReaper = new QuickTempReaper().start();

    public static final long QT_NO_DELETE = -1L;

    public static File quickTemp() { return quickTemp(TimeUnit.MINUTES.toMillis(5)); }

    public static File quickTemp(final long killAfter) {
        try {
            final File temp = temp();
            if (killAfter > 0) {
                long killTime = killAfter + now();
                killAfter(temp, killTime);
            }
            return temp;
        } catch (IOException e) {
            return die("quickTemp: cannot create temp file: " + e, e);
        }
    }

    public static void killAfter(File temp, long killTime) { killAt(temp, killTime + now()); }

    private static void killAt(File temp, long t) { qtReaper.add(temp, t); }

    private static File temp() throws IOException {
        return File.createTempFile("quickTemp-", ".tmp", getDefaultTempDir());
    }

    private interface TempDirOverrides { boolean delete(); }

    @Delegate(excludes=TempDirOverrides.class)
    private final File file;

    public TempDir () { this("700"); }

    public TempDir (File dir) { this(dir, "700", !isWindows()); }
    public TempDir (File dir, boolean doChmod) { this(dir, "700", doChmod); }

    public TempDir (String chmod) { this(null, chmod, !isWindows()); }

    public TempDir (File dir, String chmod) { this(dir, chmod, !isWindows()); }

    public TempDir (File dir, String chmod, boolean doChmod) {
        super(abs(_tempdir(dir)));
        file = new File(super.getPath());
        if (doChmod) {
            if (isWindows()) die("TempDir: chmod not supported on Windows");
            chmod(file, chmod);
        }
    }

    private static Path _tempdir(File dir) {
        try {
            final Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwx------");
            return dir == null
                    ? Files.createTempDirectory( "tempDir", PosixFilePermissions.asFileAttribute(perms))
                    : Files.createTempDirectory(dir.toPath(), "tempDir", PosixFilePermissions.asFileAttribute(perms));
        } catch (IOException e) {
            return die("_tempdir: "+shortError(e), e);
        }
    }

    public void killAfter (long t) {
        killAfter(this, t);
    }

    @Override public void close() throws IOException {
        if (!delete()) log.warn("close: error deleting TempDir: "+abs(file));
    }

    /**
     * Override to call 'delete', delete the entire directory.
     * @return true if the delete was successful.
     */
    @Override public boolean delete() { return FileUtils.deleteQuietly(file); }

}
