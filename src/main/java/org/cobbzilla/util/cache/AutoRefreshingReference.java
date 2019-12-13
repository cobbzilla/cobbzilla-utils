package org.cobbzilla.util.cache;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

public abstract class AutoRefreshingReference<T> {

    @Getter private final AtomicReference<T> object = new AtomicReference<>();
    @Getter private final AtomicLong lastSet = new AtomicLong();

    public abstract T refresh();
    public abstract long getTimeout();

    public T get() {
        synchronized (object) {
            if (isEmpty() || now() - lastSet.get() > getTimeout()) update();
            return object.get();
        }
    }

    public boolean isEmpty() { synchronized (object) { return object.get() == null; } }

    public void update() {
        synchronized (object) {
            object.set(refresh());
            lastSet.set(now());
        }
    }

    public void flush() { set(null); }

    public void set(T thing) {
        synchronized (object) {
            object.set(thing);
            lastSet.set(now());
        }
    }

}
