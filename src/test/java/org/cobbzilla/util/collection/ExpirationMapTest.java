package org.cobbzilla.util.collection;

import junit.framework.TestCase;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.system.Sleep.sleep;

public class ExpirationMapTest extends TestCase {

    public void testExpiration() {
        final var halfExpiration = SECONDS.toMillis(1);
        final var map = new ExpirationMap<String, Long>(halfExpiration * 2);

        map.put("t1", now());
        assertEquals(1, map.size());
        assertNotNull(map.get("t1"));
        assertNotNull(map.get("t1")); // reading cached again

        sleep(halfExpiration);

        map.put("t2", now());
        assertEquals(2, map.size());
        assertNotNull(map.get("t1"));
        assertNotNull(map.get("t1")); // reading cached again
        assertNotNull(map.get("t2"));
        assertNotNull(map.get("t2")); // reading cached again

        sleep(halfExpiration + 1); // add 1ms just in case ...

        assertEquals(1, map.size());
        assertNotNull(map.get("t2"));
        assertNotNull(map.get("t2")); // reading cached again

        // note that previous cleaning has been just done, and so waiting for another full interval here
        // (+1ms just in case)
        sleep(halfExpiration * 2 + 1);
        assertEquals(0, map.size());
    }
}