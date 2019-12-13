package org.cobbzilla.util.time;

import org.junit.Test;

import java.util.Collection;

import static org.cobbzilla.util.json.JsonUtil.COMPACT_MAPPER;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.time.UnicodeTimezone.getAllUnicodeTimezones;
import static org.junit.Assert.assertNotNull;

public class LinuxTimezoneTest {

    @Test public void ensureLinuxTimezoneExistsForEveryUnicodeTimezone () throws Exception {
        final Collection<UnicodeTimezone> allUnicode = getAllUnicodeTimezones();
        for (UnicodeTimezone utz : allUnicode) {
            if (utz.deprecated()) continue; // skip deprecated time zones, we may not have a mapping and that is OK
            final LinuxTimezone linuxForUnicode = LinuxTimezone.fromUnicode(utz);
            assertNotNull("no LinuxTimezone found for utz="+json(utz, COMPACT_MAPPER), linuxForUnicode);
        }
    }
}
