package org.cobbzilla.util.daemon;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class DaemonThreadFactory implements ThreadFactory {

    public static final DaemonThreadFactory instance = new DaemonThreadFactory();

    @Override public Thread newThread(Runnable r) {
        final Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    }

    public static ExecutorService fixedPool (int count) {
        if (count <= 0) {
            log.warn("fixedPool: invalid count ("+count+"), using single thread");
            count = 1;
        }
        return Executors.newFixedThreadPool(count, instance);
    }

}
