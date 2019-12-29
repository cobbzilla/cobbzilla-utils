package org.cobbzilla.util.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

@AllArgsConstructor @Accessors(chain=true)
public class JarLister {

    @Getter private final JarFile jar;
    @Getter private final Pattern pattern;
    @Getter private final Function<String, String> nameMapper;

    public JarLister (File jar, String pattern, Function<String, String> nameMapper) throws IOException {
        this.jar = new JarFile(jar);
        this.pattern = Pattern.compile(pattern);
        this.nameMapper = nameMapper;
    }

    public JarLister (File jar, String pattern) throws IOException { this(jar, pattern, Function.identity()); }

    public List<String> list() {
        final Enumeration<JarEntry> entries = jar.entries();
        final List<String> found = new ArrayList<>();
        while (entries.hasMoreElements()) {
            final JarEntry jarEntry = entries.nextElement();
            final String name = jarEntry.getName();
            if (pattern.matcher(name).matches()) {
                found.add(nameMapper.apply(name));
            }
        }
        return found;
    }

}
