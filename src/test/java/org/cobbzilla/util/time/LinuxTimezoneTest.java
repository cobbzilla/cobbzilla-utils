package org.cobbzilla.util.time;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.cobbzilla.util.json.JsonUtil.COMPACT_MAPPER;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.time.UnicodeTimezone.getAllUnicodeTimezones;
import static org.junit.Assert.assertTrue;

public class LinuxTimezoneTest {

    @Test public void ensureLinuxTimezoneExistsForEveryUnicodeTimezone () throws Exception {
        final Collection<UnicodeTimezone> allUnicode = getAllUnicodeTimezones();
        final List<UnicodeTimezone> missing = new ArrayList<>();
        for (UnicodeTimezone utz : allUnicode) {
            if (utz.deprecated()) continue; // skip deprecated time zones, we may not have a mapping and that is OK
            try {
                final LinuxTimezone linuxForUnicode = LinuxTimezone.fromUnicode(utz);
                if (linuxForUnicode == null) missing.add(utz);
            } catch (Exception e) {
                missing.add(utz);
            }
        }
        assertTrue("no LinuxTimezone found for Unicode zones=" + json(missing, COMPACT_MAPPER), missing.isEmpty());
    }
}
