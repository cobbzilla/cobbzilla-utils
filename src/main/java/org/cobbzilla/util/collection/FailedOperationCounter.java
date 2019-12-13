package org.cobbzilla.util.collection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Slf4j
public class FailedOperationCounter<T> extends ConcurrentHashMap<T, Map<Long, Long>> {

    @Getter @Setter private long expiration = TimeUnit.MINUTES.toMillis(5);
    @Getter @Setter private int maxFailures = 1;

    public void fail(T value) {
        Map<Long, Long> failures = get(value);
        if (failures == null) {
            failures = new ConcurrentHashMap<>();
            put(value, failures);
        }
        final long ftime = now();
        failures.put(ftime, ftime);
    }

    public boolean tooManyFailures(T value) {
        final Map<Long, Long> failures = get(value);
        if (failures == null) return false;
        int count = 0;
        for (Iterator<Long> iter = failures.keySet().iterator(); iter.hasNext();) {
            Long ftime = iter.next();
            if (now() - ftime > expiration) {
                iter.remove();
            } else {
                if (++count >= maxFailures) return true;
            }
        }
        return false;
    }
}
