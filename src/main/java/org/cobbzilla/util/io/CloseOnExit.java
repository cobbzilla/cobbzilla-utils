package org.cobbzilla.util.io;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CloseOnExit implements Runnable {

    private static List<Closeable> closeables = new ArrayList<>();

    private CloseOnExit () {}

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new CloseOnExit()));
    }

    @Override public void run() {
        if (closeables != null) {
            for (Closeable c : closeables) {
                try {
                    c.close();
                } catch (Exception e) {
                    log.error("Error closing: " + c + ": " + e, e);
                }
            }
        }
    }

    public static void add(Closeable closeable) { closeables.add(closeable); }

}
