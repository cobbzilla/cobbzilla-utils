package org.cobbzilla.util.system;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.cobbzilla.util.daemon.ZillaRuntime.shortError;

/**
 * Wraps an Atomic Boolean that can only go from false to true.
 * Useful when you have a state where checking the current status is expensive,
 * but once some condition evaluates to true, it is true forever thereafter.
 */
@Slf4j
public class OneWayFlag extends AtomicBoolean {

    private final String name;
    private final Callable<Boolean> check;

    public OneWayFlag(String name, Callable<Boolean> check) {
        this.name = name;
        this.check = check;
        try {
            set(check.call());
        } catch (Exception e) {
            log.warn("OneWayFlag("+name+") constructor check failed (evaluates to false): "+shortError(e));
            set(false);
        }
    }

    public boolean check () {
        if (get()) return true;
        final boolean ok;
        try {
            ok = check.call();
        } catch (Exception e) {
            log.warn("OneWayFlag("+name+") check failed (evaluates to false): "+shortError(e));
            return false;
        }
        if (ok) set(true);
        return ok;
    }

}
