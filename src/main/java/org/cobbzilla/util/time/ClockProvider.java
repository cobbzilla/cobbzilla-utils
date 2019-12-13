package org.cobbzilla.util.time;

import org.cobbzilla.util.daemon.ZillaRuntime;

public interface ClockProvider {

    long now ();

    ClockProvider SYSTEM = new ClockProvider() { @Override public long now() { return System.currentTimeMillis(); } };
    ClockProvider ZILLA  = new ClockProvider() { @Override public long now() { return ZillaRuntime.now(); } };

}
