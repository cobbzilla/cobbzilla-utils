package org.cobbzilla.util.time;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTimeZone;

import static org.cobbzilla.util.io.StreamUtil.stream2string;

@Slf4j
public class DefaultTimezone {

    public static final String DEFAULT_TIMEZONE = "US/Eastern";

    @Getter(lazy=true) private static final DateTimeZone zone = initTimeZone();

    private static DateTimeZone initTimeZone() {
        // first line that does not start with '#' within 'timezone.txt' resource file will be used
        try {
            final String[] lines = stream2string("timezone.txt").split("\n");
            for (String line : lines) if (!line.trim().startsWith("#")) return DateTimeZone.forID(line.trim());
            log.warn("initTimeZone: error, timezone.txt resource did not contain a valid timezone line, using default: "+DEFAULT_TIMEZONE);
            return DateTimeZone.forID(DEFAULT_TIMEZONE);

        } catch (Exception e) {
            log.warn("initTimeZone: error, returning default ("+DEFAULT_TIMEZONE+"): "+e.getClass().getSimpleName()+": "+e.getMessage());
            return DateTimeZone.forID(DEFAULT_TIMEZONE);
        }
    }

}
