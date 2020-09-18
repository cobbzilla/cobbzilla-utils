package org.cobbzilla.util.system;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.cobbzilla.util.daemon.ZillaRuntime.shortError;

@AllArgsConstructor @Slf4j
public class OutOfMemoryErrorUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    public static final OutOfMemoryErrorUncaughtExceptionHandler EXIT_ON_OOME = new OutOfMemoryErrorUncaughtExceptionHandler();

    private final int status;

    public OutOfMemoryErrorUncaughtExceptionHandler() { status = 2; }

    @Override public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof OutOfMemoryError) {
            try {
                log.error("!!!!! OutOfMemoryError: calling System.exit("+status+")", e);
            } catch (Throwable ignored) {}
            System.exit(status);
        } else {
            log.error("!!!!! Uncaught Exception: " + shortError(e));
        }
    }

}
