package org.cobbzilla.util.time;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.cobbzilla.util.string.StringUtil;

import java.util.*;

import static java.util.stream.Collectors.toCollection;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStream;
import static org.cobbzilla.util.string.StringUtil.getPackagePath;

@ToString
public class UnicodeTimezone implements Comparable<UnicodeTimezone> {

    // we exclude these oddball timezones, since they have no good mapping to Linux timezones
    public static final String ETC_GMT_PREFIX = "Etc/GMT";

    @Getter @Setter private String name;
    @Getter @Setter private String description;
    @Getter @Setter private String alias;
    @Getter @Setter private Boolean deprecated;
    @Getter @Setter private String preferred;
    @Getter @Setter private String since;

    public String firstAlias() {
        return empty(alias) ? StringUtil.EMPTY : aliases()[0];
    }

    public String[] aliases() {
        return empty(alias) ? StringUtil.EMPTY_ARRAY : alias.split("\\s+");
    }

    public boolean deprecated() { return deprecated != null && deprecated; }

    @Override public int compareTo(UnicodeTimezone other) { return firstAlias().compareTo(other.firstAlias()); }

    public LinuxTimezone toLinux () { return LinuxTimezone.fromUnicode(this); }
    public JavaTimezone toJava () { return JavaTimezone.fromUnicode(this); }

    public static UnicodeTimezone fromString(String value) { return getUnicodeTimezoneMap().get(value); }

    @Getter(lazy=true) private static final Set<UnicodeTimezone> allUnicodeTimezones = initAllTimezones();
    private static Set<UnicodeTimezone> initAllTimezones () {
        try {
            final String path = getPackagePath(UnicodeTimezone.class) + "/unicode-timezones.xml";
            final UnicodeXmlDocument document = new XmlMapper().readValue(loadResourceAsStream(path), UnicodeXmlDocument.class);
            return Arrays.stream(document.getKeyword().getKey().getType())
                    .filter(tz -> !tz.deprecated() && !tz.firstAlias().startsWith(ETC_GMT_PREFIX))
                    .collect(toCollection(TreeSet::new));
        } catch (Exception e) {
            return die("initAllTimezones: "+e);
        }
    }

    @Getter(lazy=true) private static final Map<String, UnicodeTimezone> unicodeTimezoneMap = initTimezoneMap();
    private static Map<String, UnicodeTimezone> initTimezoneMap() {
        final Map<String, UnicodeTimezone> map = new LinkedHashMap<>();
        getAllUnicodeTimezones().stream().filter(tz -> !tz.deprecated()).forEach(tz -> {
            for (String alias : tz.aliases()) map.put(alias, tz);
        });
        return map;
    }

    public static class UnicodeXmlDocument {
        @Getter @Setter private UnicodeVersion version;
        @Getter @Setter private UnicodeKeyword keyword;
    }
    public static class UnicodeVersion {
        @Getter @Setter private String number;
    }
    public static class UnicodeKeyword {
        @Getter @Setter private UnicodeKey key;
    }
    public static class UnicodeKey {
        @Getter @Setter private String name;
        @Getter @Setter private String description;
        @Getter @Setter private String alias;

        @JacksonXmlElementWrapper(useWrapping=false)
        @Getter @Setter private UnicodeTimezone[] type;
    }

}
