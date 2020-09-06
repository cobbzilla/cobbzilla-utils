package org.cobbzilla.util.daemon;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.Executors.newFixedThreadPool;

@Slf4j @NoArgsConstructor
public class DaemonThreadFactory implements ThreadFactory {

    public static final DaemonThreadFactory instance = new DaemonThreadFactory();

    public DaemonThreadFactory (String name) { this.name = name+"-"; }

    private String name = "DaemonThread-";
    private final AtomicInteger counter = new AtomicInteger();

    @Override public Thread newThread(Runnable r) {
        final Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName(name+counter.incrementAndGet());
        return t;
    }

    public static ExecutorService fixedPool (int count, String name) {
        if (count <= 0) {
            log.warn("fixedPool: invalid count ("+count+"), using single thread");
            count = 1;
        }
        return newFixedThreadPool(count, new DaemonThreadFactory(name));
    }

    public static ExecutorService fixedPool (int count) {
        if (count <= 0) {
            log.warn("fixedPool: invalid count ("+count+"), using single thread");
            count = 1;
        }
        return newFixedThreadPool(count, instance);
    }

}
