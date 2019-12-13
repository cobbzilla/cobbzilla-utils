package org.cobbzilla.util.io;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DeleteOnExit implements Runnable {

    private static List<File> paths = new ArrayList<>();

    private DeleteOnExit () {}

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new DeleteOnExit()));
    }

    @Override public void run() {
        for (File path : paths) {
            if (!path.exists()) return;
            if (path.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(path);
                } catch (IOException e) {
                    log.warn("FileUtil.deleteOnExit: error deleting path=" + path + ": " + e, e);
                }
            } else {
                if (!path.delete()) {
                    log.warn("FileUtil.deleteOnExit: error deleting path=" + path);
                }
            }
        }
    }

    public static void add(File path) { paths.add(path); }

}
