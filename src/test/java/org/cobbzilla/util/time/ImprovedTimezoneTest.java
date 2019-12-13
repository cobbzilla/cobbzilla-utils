package org.cobbzilla.util.time;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

@Slf4j
public class ImprovedTimezoneTest {

    @Test
    public void validate () {

        if (!System.getProperty("os.name").toLowerCase().contains("linux")) {
            log.warn("validate: skipping, this only runs on Linux");
        }

        // validates that all Java timezones properly map to Linux timezones, and those timezones exist
        for (ImprovedTimezone tz : ImprovedTimezone.getTimeZones()) {
            assertTrue(new File("/usr/share/zoneinfo/"+tz.getLinuxName()).exists());
        }
    }

}
