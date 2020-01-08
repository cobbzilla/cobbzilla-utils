package org.cobbzilla.util.daemon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ExceptionHandler {

    void handle(Exception e);

    Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    ExceptionHandler DEFAULT_EX_RUNNABLE = e -> log.error("Error: " + e);

    static ExceptionHandler exceptionRunnable(Class<? extends Throwable>[] fatalExceptionClasses) {
        return e -> {
            for (Class<? extends Throwable> c : fatalExceptionClasses) {
                if (c.isAssignableFrom(e.getClass())) {
                    if (e instanceof RuntimeException) throw (RuntimeException) e;
                    ZillaRuntime.die("fatal exception: "+e);
                }
            }
            DEFAULT_EX_RUNNABLE.handle(e);
        };
    }
}
