package org.cobbzilla.util.time;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.string.StringUtil.getPackagePath;

@AllArgsConstructor
public class JavaTimezone {

    @Getter private final TimeZone timeZone;

    @Getter(lazy=true) private static final List<JavaTimezone> allJavaTimezones = initAllTimezones();
    private static List<JavaTimezone> initAllTimezones() {
        return Arrays.stream(TimeZone.getAvailableIDs()).map(tz -> new JavaTimezone(TimeZone.getTimeZone(tz))).collect(Collectors.toList());
    }

    @Getter(lazy=true) private static final Map<String, JavaTimezone> javaTimezoneMap = initTimezoneMap();
    private static Map<String, JavaTimezone> initTimezoneMap() {
        final Map<String, JavaTimezone> map = getAllJavaTimezones().stream().collect(toMap(tz -> tz.getTimeZone().getID(), identity()));
        final String path = getPackagePath(UnicodeTimezone.class) + "/java-timezone-exceptions.json";
        final Map<String, String> exceptions = json(loadResourceAsStringOrDie(path), LinkedHashMap.class);
        for (Map.Entry<String, String> exc : exceptions.entrySet()) {
            if (!map.containsKey(exc.getValue())) return die("initTimezoneMap: error reading "+path+": JavaTimezone not found: "+exc.getValue());
            map.put(exc.getKey(), map.get(exc.getValue()));
        }
        return map;
    }

    public static JavaTimezone fromUnicode(UnicodeTimezone utz) {
        final Map<String, JavaTimezone> map = getJavaTimezoneMap();
        for (String alias : utz.aliases()) {
            if (map.containsKey(alias)) return map.get(alias);
        }
        return die("fromUnicode: no JavaTimezone found for: "+utz);
    }

    public static JavaTimezone fromLinux(LinuxTimezone ltz) {
        final JavaTimezone javaTimezone = getJavaTimezoneMap().get(ltz.getName());
        if (javaTimezone == null) {
            return die("fromLinux: no JavaTimezone found for: "+ltz.getName());
        }
        return javaTimezone;
    }

    public static JavaTimezone fromString(String value) { return getJavaTimezoneMap().get(value); }

}
