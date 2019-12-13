package org.cobbzilla.util.time;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

@NoArgsConstructor
public class CurrentTime {

    @Getter @Setter private String zone;
    @Getter @Setter private CurrentTimeValues now;
    @Getter @Setter private CurrentTimeValues realNow;

    public CurrentTime(DateTimeZone tz) {
        zone = tz.getID();
        now = new CurrentTimeValues(tz, ZillaRuntime.now());
        realNow = ZillaRuntime.getSystemTimeOffset() == 0 ? null : new CurrentTimeValues(tz, ZillaRuntime.realNow());
    }

    @NoArgsConstructor
    public static class CurrentTimeValues {
        @Getter @Setter private long now;
        @Getter @Setter private String yyyyMMdd;
        @Getter @Setter private String yyyyMMddHHmmss;

        public CurrentTimeValues(DateTimeZone tz, long now) {
            this.now = now;
            final DateTime time = new DateTime(now, tz);
            yyyyMMdd = TimeUtil.DATE_FORMAT_YYYY_MM_DD.print(time);
            yyyyMMddHHmmss = TimeUtil.DATE_FORMAT_YYYY_MM_DD_HH_mm_ss.print(time);
        }
    }

}
