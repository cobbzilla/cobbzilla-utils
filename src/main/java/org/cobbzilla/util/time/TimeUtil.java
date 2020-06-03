package org.cobbzilla.util.time;

import org.cobbzilla.util.string.StringUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.DurationFieldType;
import org.joda.time.format.*;

import static java.util.concurrent.TimeUnit.*;
import static org.apache.commons.lang3.LocaleUtils.toLocale;
import static org.cobbzilla.util.daemon.ZillaRuntime.*;

public class TimeUtil {

    public static final long DAY    = DAYS.toMillis(1);
    public static final long HOUR   = HOURS.toMillis(1);
    public static final long MINUTE = MINUTES.toMillis(1);
    public static final long SECOND = SECONDS.toMillis(1);

    public static final DateTimeFormatter DATE_FORMAT_MMDDYYYY = DateTimeFormat.forPattern("MM/dd/yyyy");
    public static final DateTimeFormatter DATE_FORMAT_MMMM_D_YYYY = DateTimeFormat.forPattern("MMMM d, yyyy");
    public static final DateTimeFormatter DATE_FORMAT_MMMM_D_YYYY2 = DateTimeFormat.forPattern("MMMM d yyyy");
    public static final DateTimeFormatter DATE_FORMAT_YYYY_MM_DD = DateTimeFormat.forPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_FORMAT_YYYY_MM = DateTimeFormat.forPattern("yyyy-MM");
    public static final DateTimeFormatter DATE_FORMAT_YYYY = DateTimeFormat.forPattern("yyyy");
    public static final DateTimeFormatter DATE_FORMAT_YYYYMM = DateTimeFormat.forPattern("yyyyMM");
    public static final DateTimeFormatter DATE_FORMAT_YYYYMMDD = DateTimeFormat.forPattern("yyyyMMdd");
    public static final DateTimeFormatter DATE_FORMAT_YYYY_MMM = DateTimeFormat.forPattern("yyyy MMM");
    public static final DateTimeFormatter DATE_FORMAT_YYYY_MMM_DD = DateTimeFormat.forPattern("yyyy MMM dd");
    public static final DateTimeFormatter DATE_FORMAT_MMM_DD_YYYY = DateTimeFormat.forPattern("MMM dd, yyyy");
    public static final DateTimeFormatter DATE_FORMAT_YYYY_MM_DD_HH_mm_ss = DateTimeFormat.forPattern("yyyy-MM-dd-HH-mm-ss");
    public static final DateTimeFormatter DATE_FORMAT_YYYY_MM_DD_HH_mm_ss_SSS = DateTimeFormat.forPattern("yyyy-MM-dd-HH-mm-ss-SSS");
    public static final DateTimeFormatter DATE_FORMAT_YYYYMMDDHHMMSS = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    public static final DateTimeFormatter DATE_FORMAT_YYYY_MM_DD_HH  = DateTimeFormat.forPattern("yyyy-MM-dd-HH");
    public static final DateTimeFormatter DATE_FORMAT_HYPHEN_MMDDYYYY = DateTimeFormat.forPattern("MM-dd-yyyy");
    public static final DateTimeFormatter DATE_FORMAT_HYPHEN_MMMDDYYYY = DateTimeFormat.forPattern("MMM-dd-yyyy");
    public static final DateTimeFormatter DATE_FORMAT_EEE_DD_MMM_YYYY_HH_MM_SS_ZZZ = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss zzz");
    public static final DateTimeFormatter DATE_FORMAT_ISO_8601 = ISODateTimeFormat.dateTimeParser();
    public static final DateTimeFormatter DATE_FORMAT_IF_MODIFIED_SINCE = DATE_FORMAT_EEE_DD_MMM_YYYY_HH_MM_SS_ZZZ;
    public static final DateTimeFormatter DATE_FORMAT_LAST_MODIFIED = DATE_FORMAT_IF_MODIFIED_SINCE;

    public static final DateTimeFormatter[] DATE_TIME_FORMATS = {
            DATE_FORMAT_YYYY_MM_DD_HH_mm_ss,
            DATE_FORMAT_YYYY_MM_DD, DATE_FORMAT_YYYY_MM, DATE_FORMAT_YYYY, DATE_FORMAT_MMMM_D_YYYY2,
            DATE_FORMAT_YYYYMMDD, DATE_FORMAT_YYYYMM,
            DATE_FORMAT_YYYYMMDDHHMMSS, DATE_FORMAT_MMMM_D_YYYY,
            DATE_FORMAT_YYYY_MMM, DATE_FORMAT_YYYY_MMM_DD,
            DATE_FORMAT_HYPHEN_MMDDYYYY, DATE_FORMAT_HYPHEN_MMMDDYYYY, DATE_FORMAT_MMDDYYYY
    };

    // For now only m (months) and d (days) are supported
    // Both have to be present at the same time in that same order, but the value for each can be 0 to exclude that one - e.g. 0m15d.
    public static final PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
            .appendMonths().appendSuffix("m").appendDays().appendSuffix("d").toFormatter();

    public static Long parse(String time, DateTimeFormatter formatter) {
        return empty(time) ? null : formatter.parseDateTime(time).getMillis();
    }

    public static Long parse(String time, DateTimeFormatter formatter, DateTimeZone timeZone) {
        return empty(time) ? null : formatter.withZone(timeZone).parseDateTime(time).getMillis();
    }

    public static Long parseWithLocale(String time, DateTimeFormatter formatter, String locale) {
        return empty(time) ? null : formatter.withLocale(toLocale(locale)).parseDateTime(time).getMillis();
    }

    public static Long parse(String val) {
        for (DateTimeFormatter f : DATE_TIME_FORMATS) {
            try {
                return TimeUtil.parse(val, f);
            } catch (Exception ignored) {
                // noop
            }
        }
        return null;
    }

    public static Long parseISO8601(String val) { return DATE_FORMAT_ISO_8601.parseMillis(val); }
    public static String formatISO8601(long val) { return new DateTime(val).toString(); }

    public static Long parseWithLocale(String val, String locale) {
        for (DateTimeFormatter f : DATE_TIME_FORMATS) {
            try {
                return TimeUtil.parseWithLocale(val, f, locale);
            } catch (Exception ignored) {
                // noop
            }
        }
        return null;
    }

    public static Long parse(String val, DateTimeZone timeZone) {
        for (DateTimeFormatter f : DATE_TIME_FORMATS) {
            try {
                return TimeUtil.parse(val, f, timeZone);
            } catch (Exception ignored) {
                // noop
            }
        }
        return null;
    }

    public static String format(Long time, DateTimeFormatter formatter) {
        return time == null ? null : new DateTime(time).toString(formatter);
    }

    public static String formatDurationFrom(long start) {
        long duration = now() - start;
        return formatDuration(duration);
    }

    public static String formatDuration(long duration) {
        final boolean negative = duration < 0;
        if (negative) duration *= -1L;
        final String prefix = negative ? "-" : "";

        long days = 0, hours = 0, mins = 0, secs = 0, millis = 0;

        if (duration > DAY) {
            days = duration/DAY;
            duration -= days * DAY;
        }
        if (duration > HOUR) {
            hours = duration/HOUR;
            duration -= hours * HOUR;
        }
        if (duration > MINUTE) {
            mins = duration/MINUTE;
            duration -= mins * MINUTE;
        }
        if (duration > SECOND) {
            secs = duration/SECOND;
        }
        millis = duration - secs * SECOND;

        if (days > 0) return prefix+String.format("%1$01dd %2$02d:%3$02d:%4$02d.%5$04d", days, hours, mins, secs, millis);
        return prefix+String.format("%1$02d:%2$02d:%3$02d.%4$04d", hours, mins, secs, millis);
    }

    public static long parseDuration(String duration) {
        if (empty(duration)) return 0;
        final long val = Long.parseLong(duration.length() > 1 ? StringUtil.chopSuffix(duration) : duration);
        switch (duration.charAt(duration.length()-1)) {
            case 's': return SECONDS.toMillis(val);
            case 'm': return MINUTES.toMillis(val);
            case 'h': return HOURS.toMillis(val);
            case 'd': return DAYS.toMillis(val);
            default: return val;
        }
    }

    public static long addYear (long time) {
        return new DateTime(time).withFieldAdded(DurationFieldType.years(), 1).getMillis();
    }

    public static long add365days (long time) {
        return new DateTime(time).withFieldAdded(DurationFieldType.days(), 365).getMillis();
    }

    public static String timestamp() { return timestamp(ClockProvider.ZILLA); }

    public static String timestamp(ClockProvider clock) {
        final long now = clock.now();
        return DATE_FORMAT_YYYY_MM_DD.print(now)+"-"+hexnow(now);
    }

    public static long startOfWeekMillis() { return startOfWeek().getMillis();  }
    public static DateTime startOfWeek() { return startOfWeek(DefaultTimezone.getZone()); }
    public static DateTime startOfWeek(DateTimeZone zone) {
        final DateTime startOfToday = new DateTime(zone).withTimeAtStartOfDay();
        return startOfToday.withFieldAdded(DurationFieldType.days(), -1 * startOfToday.getDayOfWeek());
    }

    public static long startOfMonthMillis() { return startOfMonth().getMillis();  }
    public static DateTime startOfMonth() { return startOfMonth(DefaultTimezone.getZone()); }
    public static DateTime startOfMonth(DateTimeZone zone) {
        final DateTime startOfToday = new DateTime(zone).withTimeAtStartOfDay();
        return startOfToday.withFieldAdded(DurationFieldType.days(), -1 * startOfToday.getDayOfMonth());
    }

    public static DateTime startOfQuarter(DateTime t) {
        final int month = t.getMonthOfYear();
        if (month <= 3) return t.withMonthOfYear(1);
        if (month <= 6) return t.withMonthOfYear(4);
        if (month <= 9) return t.withMonthOfYear(7);
        return t.withMonthOfYear(10);
    }

    public static long startOfQuarterMillis() { return startOfQuarter().getMillis();  }
    public static DateTime startOfQuarter() { return startOfQuarter(DefaultTimezone.getZone()); }
    public static DateTime startOfQuarter(DateTimeZone zone) { return startOfQuarter(new DateTime(zone).withTimeAtStartOfDay()); }

    public static long startOfYearMillis() { return startOfYear().getMillis();  }
    public static DateTime startOfYear() { return startOfYear(DefaultTimezone.getZone()); }
    public static DateTime startOfYear(DateTimeZone zone) { return new DateTime(zone).withTimeAtStartOfDay().withMonthOfYear(1).withDayOfMonth(1); }

    public static long yesterdayMillis() { return yesterday().getMillis();  }
    public static DateTime yesterday() { return yesterday(DefaultTimezone.getZone()); }
    public static DateTime yesterday(DateTimeZone zone) { return new DateTime(zone).withTimeAtStartOfDay().withFieldAdded(DurationFieldType.days(), -1); }

    public static long lastWeekMillis() { return lastWeek().getMillis();  }
    public static DateTime lastWeek() { return lastWeek(DefaultTimezone.getZone()); }
    public static DateTime lastWeek(DateTimeZone zone) {
        return new DateTime(zone).withTimeAtStartOfDay().withFieldAdded(DurationFieldType.days(), -7).withDayOfWeek(1);
    }

    public static long lastMonthMillis() { return lastMonth().getMillis();  }
    public static DateTime lastMonth() { return lastMonth(DefaultTimezone.getZone()); }
    public static DateTime lastMonth(DateTimeZone zone) {
        return new DateTime(zone).withTimeAtStartOfDay().withFieldAdded(DurationFieldType.months(), -1).withDayOfMonth(1);
    }

    public static long lastQuarterMillis() { return lastQuarter().getMillis();  }
    public static DateTime lastQuarter() { return lastQuarter(DefaultTimezone.getZone()); }
    public static DateTime lastQuarter(DateTimeZone zone) {
        return startOfQuarter(new DateTime(zone).withTimeAtStartOfDay().withFieldAdded(DurationFieldType.months(), -3));
    }

    public static long lastYearMillis() { return lastYear().getMillis();  }
    public static DateTime lastYear() { return lastYear(DefaultTimezone.getZone()); }
    public static DateTime lastYear(DateTimeZone zone) {
        return new DateTime(zone).withTimeAtStartOfDay().withFieldAdded(DurationFieldType.years(), -1).withDayOfYear(1);
    }

    public static boolean isTimestampInFuture(long t) { return t > now(); }
    public static boolean isTimestampInPast(long t) { return t < now(); }
}
