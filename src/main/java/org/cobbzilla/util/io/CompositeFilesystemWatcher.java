package org.cobbzilla.util.io;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.string.StringUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;

@Slf4j
public abstract class CompositeFilesystemWatcher<T extends FilesystemWatcher> implements Closeable {

    private Map<Path, T> watchers = new ConcurrentHashMap<>();

    @Override public void close() throws IOException {
        Map<Path, T> copy = watchers;
        watchers = null;
        for (T watcher : copy.values()) {
            watcher.close();
        }
    }

    public List<Path> pathsWatching () { return new ArrayList<>(watchers.keySet()); }

    public List<File> dirsWatching() {
        List<Path> paths = pathsWatching();
        List<File> dirs = new ArrayList<>(paths.size());
        for (Path p : paths) dirs.add(p.toFile());
        return dirs;
    }

    public boolean isEmpty() { return watchers.isEmpty(); }

    protected abstract T newWatcher(Path path);

    public void add (String path) { add(new File(path)); }

    public void add (File path) { add(path.toPath()); }

    public void add (Path path) {
        final T watcher = newWatcher(path);
        final T old = watchers.remove(path);
        if (old != null) {
            log.warn("Replacing old watcher ("+old+") with new one: "+watcher);
            old.stop();
        }
        watcher.start();
        watchers.put(path, watcher);
    }

    public void addAll(File[] paths)   { if (paths != null) for (File p : paths) add(p); }
    public void addAll(Path[] paths)   { if (paths != null) for (Path p : paths) add(p); }
    public void addAll(String[] paths) { if (paths != null) for (String p : paths) add(new File(p)); }

    public void addAll(Collection things) {
        if (!things.isEmpty()) {
            final Class<?> clazz = things.iterator().next().getClass();
            if (clazz.equals(File.class)) {
                addAll((File[]) things.toArray(new File[things.size()]));
            } else if (clazz.equals(Path.class)) {
                addAll((Path[]) things.toArray(new Path[things.size()]));
            } else if (clazz.equals(String.class)) {
                addAll((String[]) things.toArray(new String[things.size()]));
            }
        }
    }

    @Override public String toString() {
        return "CompositeFilesystemWatcher<"+getFirstTypeParam(getClass()).getName()+">{" +
                "paths=" + StringUtil.toString(watchers.keySet(), " ") + "}";
    }
}
