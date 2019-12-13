package org.cobbzilla.util.io;

import lombok.Cleanup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.cobbzilla.util.reflect.ReflectionUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Slf4j
public class JarTrimmer {

    public static final String CLASS_SUFFIX = ".class";
    @Getter private IncludeCount counter = new IncludeCount("");

    public static class IncludeCount {
        @Getter private int count;

        public int getTotalCount () {
            int total = count;
            for (IncludeCount count : subPaths.values()) total += count.getTotalCount();
            return total;
        }

        @Getter private String path;

        @Getter private Map<String, IncludeCount> subPaths;

        public IncludeCount (String path) {
            this.path = path;
            this.count = 0;
            this.subPaths = new HashMap<>();
        }

        public void incr() { count++; }

        public IncludeCount getCounter(String path) {
            if (empty(path)) return this;
            final int slashPos = path.indexOf('/');
            final boolean hasSlash = slashPos != -1;
            final String part = hasSlash ? path.substring(0, slashPos) : path ;
            final IncludeCount subCount = subPaths.computeIfAbsent(part, v -> new IncludeCount(part));
            return hasSlash ? subCount.getCounter(path.substring(slashPos+1)) : subCount;
        }
    }

    public IncludeCount trim (JarTrimmerConfig config) throws Exception {

        final JarFile jar = new JarFile(config.getInJar());

        // walk all class resources in jar file, track location/count of required classes
        processJar(jar, jarEntry -> {
            final String name = jarEntry.getName();
            if (name.endsWith(CLASS_SUFFIX)) {
                if (config.required(name)) counter.getCounter(toPath(name)).incr();
            }
            return null;
        });

        // Level 1: any packages that do not contain ANY required classes will not be included in the output jar
        final File temp = FileUtil.temp(".jar");
        @Cleanup final JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(temp));
        final Set<String> dirsCreated = new HashSet<>();
        processJar(jar, jarEntry -> {
            final String name = jarEntry.getName();
            if (shouldInclude(config, name)) {
                try {
                    final String dir = name.contains("/") ? toPath(name) : null;
                    if (dir != null && !dirsCreated.contains(dir)) {
                        final JarEntry dirEntry = new JarEntry(dir +"/");
                        dirEntry.setTime(jarEntry.getTime());
                        jarOut.putNextEntry(dirEntry);
                        dirsCreated.add(dir);
                    }
                    final JarEntry outEntry = new JarEntry(jarEntry.getName());
                    ReflectionUtil.copy(outEntry, jarEntry, new String[] {"method", "time", "size", "compressedSize", "crc"});
                    jarOut.putNextEntry(outEntry);
                    @Cleanup final InputStream in = jar.getInputStream(jarEntry);
                    IOUtils.copy(in, jarOut);

                } catch (Exception e) {
                    return die("processJar: " + e, e);
                }
            } else if (!name.endsWith("/")) {
                log.info("omitted: "+ name);
            }
            return null;
        });

        FileUtil.renameOrDie(temp, config.getOutJar());
        return counter;
    }

    private boolean shouldInclude(JarTrimmerConfig config, String name) {
        if (name.endsWith("/")) return false;
        if (config.required(name)) return true;
        return counter.getCounter(toPath(name)).getTotalCount() > 0;
    }

    private void processJar (JarFile jar, Function<JarEntry, IncludeCount> func) {
        final Enumeration<? extends JarEntry> enumeration = jar.entries();
        while (enumeration.hasMoreElements()) func.apply(enumeration.nextElement());
    }

    private String toPath(String jarEntryName) {
        final int lastSlash = jarEntryName.lastIndexOf('/');
        return (lastSlash == -1 || lastSlash == jarEntryName.length()-1) ? jarEntryName : jarEntryName.substring(0, lastSlash);
    }

    protected static String toClassName(String jarEntryName) {
        return jarEntryName.substring(0, jarEntryName.length()-CLASS_SUFFIX.length()).replace("/", ".");
    }

}
