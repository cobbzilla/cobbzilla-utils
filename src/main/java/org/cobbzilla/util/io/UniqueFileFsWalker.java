package org.cobbzilla.util.io;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.security.ShaUtil.sha256_file;

@Slf4j
public class UniqueFileFsWalker implements FilesystemVisitor {

    @Getter private Map<String, Set<String>> hash;

    public UniqueFileFsWalker (int size) { hash = new ConcurrentHashMap<>(size); }

    @Override public void visit(File file) {
        final String path = abs(file);
        log.debug(path);
        hash.computeIfAbsent(sha256_file(file), k -> new HashSet<>()).add(path);
    }
}
