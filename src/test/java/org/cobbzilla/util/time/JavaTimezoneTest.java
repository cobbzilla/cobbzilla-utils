package org.cobbzilla.util.time;

import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.cobbzilla.util.json.JsonUtil.COMPACT_MAPPER;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.time.UnicodeTimezone.getAllUnicodeTimezones;
import static org.junit.Assert.assertNotNull;

public class JavaTimezoneTest {

    @Test public void ensureJavaTimezoneExistsForEveryUnicodeTimezone () throws Exception {
        final Collection<UnicodeTimezone> allUnicode = getAllUnicodeTimezones();
        for (UnicodeTimezone utz : allUnicode) {
            if (utz.deprecated()) continue; // skip deprecated time zones, we may not have a mapping and that is OK
            final JavaTimezone javaForUnicode = JavaTimezone.fromUnicode(utz);
            assertNotNull("no JavaTimezone found for utz="+json(utz, COMPACT_MAPPER), javaForUnicode);
        }
    }

    @Test public void ensureJavaTimezoneExistsForEveryLinuxTimezone () throws Exception {
        final List<LinuxTimezone> allLinux = LinuxTimezone.getAllLinuxTimezones();
        for (LinuxTimezone linuxTimezone : allLinux) {
            final JavaTimezone javaForLinux = JavaTimezone.fromLinux(linuxTimezone);
            assertNotNull("no JavaTimezone found for linuxTimezone="+linuxTimezone.getName(), javaForLinux);
        }
    }

}
