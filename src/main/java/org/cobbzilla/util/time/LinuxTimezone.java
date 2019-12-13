package org.cobbzilla.util.time;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.string.StringUtil.getPackagePath;
import static org.cobbzilla.util.system.CommandShell.execScript;

@AllArgsConstructor
public class LinuxTimezone {

    @Getter private final String name;

    public JavaTimezone toJava () { return JavaTimezone.fromLinux(this); }

    @Getter(lazy=true) private static final List<LinuxTimezone> allLinuxTimezones = initAllTimezones();
    private static List<LinuxTimezone> initAllTimezones() {
        final String[] lines = execScript("timedatectl list-timezones").split("\n");
        if (empty(lines)) die("initAllTimezones: error running timedatectl list-timezones, no output found on stdout");
        return Arrays.stream(lines).map(LinuxTimezone::new).collect(Collectors.toList());
    }

    @Getter(lazy=true) private static final Map<String, LinuxTimezone> linuxTimezoneMap = initTimezoneMap();
    private static Map<String, LinuxTimezone> initTimezoneMap() {
        final Map<String, LinuxTimezone> map = getAllLinuxTimezones().stream().collect(toMap(LinuxTimezone::getName, identity()));
        final String path = getPackagePath(UnicodeTimezone.class) + "/linux-timezone-exceptions.json";
        final Map<String, String> exceptions = json(loadResourceAsStringOrDie(path), LinkedHashMap.class);
        for (Map.Entry<String, String> exc : exceptions.entrySet()) {
            if (!map.containsKey(exc.getValue())) return die("initTimezoneMap: error reading "+path+": LinuxTimezone not found: "+exc.getValue());
            map.put(exc.getKey(), map.get(exc.getValue()));
        }
        return map;
    }

    public static LinuxTimezone fromUnicode(UnicodeTimezone utz) {
        final Map<String, LinuxTimezone> map = getLinuxTimezoneMap();
        for (String alias : utz.aliases()) {
            if (map.containsKey(alias)) return map.get(alias);
        }
        return die("fromUnicode: no LinuxTimezone found for: "+utz);
    }

}
