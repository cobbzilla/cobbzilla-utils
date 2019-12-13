package org.cobbzilla.util.io;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.util.collection.ArrayUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.cobbzilla.util.string.StringUtil.UTF8cs;

@Accessors(chain=true)
public class JarTrimmerConfig {

    @Getter @Setter private File inJar;
    @Setter private File outJar;
    public File getOutJar () { return outJar != null ? outJar : inJar; }

    @Getter @Setter private String[] requiredClasses;
    @JsonIgnore @Getter(lazy=true) private final Set<String> requiredClassSet = new HashSet<>(Arrays.asList(getRequiredClasses()));

    public JarTrimmerConfig setRequiredClassesFromFile (File f) throws IOException {
        requiredClasses = FileUtils.readLines(f, UTF8cs).toArray(new String[0]);
        return this;
    }

    @Getter @Setter private String[] requiredPrefixes = new String[] { "META-INF", "WEB-INF" };
    public JarTrimmerConfig requirePrefix(String prefix) { requiredPrefixes = ArrayUtil.append(requiredPrefixes, prefix); return this; }
    @JsonIgnore @Getter(lazy=true) private final Set<String> requiredPrefixSet = new HashSet<>(Arrays.asList(getRequiredPrefixes()));

    @Getter @Setter private boolean includeRootFiles = true;
    @Getter @Setter private File counterFile = null;
    public boolean hasCounterFile () { return counterFile != null; }

    public boolean required(String name) {
        return getRequiredClassSet().contains(JarTrimmer.toClassName(name))
                || requiredByPrefix(name)
                || (includeRootFiles && !name.contains("/"));
    }

    private boolean requiredByPrefix(String name) {
        for (String prefix : getRequiredPrefixSet()) if (name.startsWith(prefix)) return true;
        return false;
    }

    public JarTrimmerConfig requireClasses(File file) throws IOException {
        requiredClasses = ArrayUtil.append(requiredClasses, FileUtils.readLines(file, UTF8cs).toArray(new String[0]));
        return this;
    }
}
