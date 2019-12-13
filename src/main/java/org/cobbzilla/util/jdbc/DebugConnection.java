package org.cobbzilla.util.jdbc;

import lombok.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DebugConnection implements Connection {

    private static final AtomicInteger counter = new AtomicInteger(0);

    private int id;

    @Delegate private Connection delegate;

    public DebugConnection(Connection delegate) {
        this.id = counter.getAndIncrement();
        this.delegate = delegate;
        final String msg = "DebugConnection " + id + " opened from " + ExceptionUtils.getStackTrace(new Exception("opened"));
        log.info(msg);
    }

}
