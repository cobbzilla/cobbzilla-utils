package org.cobbzilla.util.time;

import com.fasterxml.jackson.annotation.JsonCreator;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public enum TimePeriod {

    seconds, minutes, hours, days, weeks, months, years;

    @JsonCreator public static TimePeriod fromString(String name) {
        if (name == null) return die("TimePeriod: null name");
        switch (name.toLowerCase()) {
            case "second": case "seconds": case "s": return seconds;
            case "minute": case "minutes": case "m": return minutes;
            case "hour": case "hours": case "h": return hours;
            case "day": case "days": case "d": return days;
            case "week": case "weeks": case "w": return weeks;
            case "month": case "months": case "M": return months;
            case "year": case "years": case "y": case "Y": return years;
            default: return die("TimePeriod: invalid name: "+name);
        }
    }

}
