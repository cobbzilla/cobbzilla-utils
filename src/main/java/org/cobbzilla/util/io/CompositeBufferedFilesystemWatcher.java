package org.cobbzilla.util.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Collection;
import java.util.List;

@AllArgsConstructor @ToString(callSuper=true, of={"timeout", "maxEvents"})
public abstract class CompositeBufferedFilesystemWatcher extends CompositeFilesystemWatcher<BufferedFilesystemWatcher> {

    @Getter private final long timeout;
    @Getter private final int maxEvents;

    public CompositeBufferedFilesystemWatcher (long timeout, int maxEvents, File[] paths) {
        this(timeout, maxEvents);
        addAll(paths);
    }

    public CompositeBufferedFilesystemWatcher (long timeout, int maxEvents, String[] paths) {
        this(timeout, maxEvents);
        addAll(paths);
    }

    public CompositeBufferedFilesystemWatcher (long timeout, int maxEvents, Path[] paths) {
        this(timeout, maxEvents);
        addAll(paths);
    }

    public CompositeBufferedFilesystemWatcher (long timeout, int maxEvents, Collection things) {
        this(timeout, maxEvents);
        addAll(things);
    }

    public abstract void fire(List<WatchEvent<?>> events);
    private void _fire(List<WatchEvent<?>> events) { fire(events); }

    @Override protected BufferedFilesystemWatcher newWatcher(Path path) {
        return new BufferedFilesystemWatcher(path, timeout, maxEvents) {
            @Override protected void fire(List<WatchEvent<?>> events) {
                _fire(events);
            }
        };
    }
}
