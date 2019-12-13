package org.cobbzilla.util.time;

import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

public interface TimeSpecifier {

    long get(long t);

    static TimeSpecifier nowSpecifier() { return t -> now(); }
    static TimeSpecifier todaySpecifier() { return t -> new DateTime(t, DefaultTimezone.getZone()).withTimeAtStartOfDay().getMillis(); }

    static TimeSpecifier pastDaySpecifier(int count) { return t -> new DateTime(t, DefaultTimezone.getZone()).withTimeAtStartOfDay().withFieldAdded(DurationFieldType.days(), -1 * count).getMillis(); }
    static TimeSpecifier yesterdaySpecifier() { return pastDaySpecifier(1); }

    static TimeSpecifier futureDaySpecifier(int count) { return t -> new DateTime(t, DefaultTimezone.getZone()).withTimeAtStartOfDay().withFieldAdded(DurationFieldType.days(), count).getMillis(); }
    static TimeSpecifier tomorrowSpecifier() { return futureDaySpecifier(1); }

}
