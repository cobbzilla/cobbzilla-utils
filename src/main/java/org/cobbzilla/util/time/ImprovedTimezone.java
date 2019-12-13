package org.cobbzilla.util.time;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.string.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Slf4j
public class ImprovedTimezone {

    @Getter private int id;
    @Getter private String gmtOffset;
    @Getter private String displayName;
    @Getter private String linuxName;
    @Getter private TimeZone timezone;
    @Getter private String displayNameWithOffset;

    private static List<ImprovedTimezone> TIMEZONES = null;
    private static Map<Integer, ImprovedTimezone> TIMEZONES_BY_ID = new HashMap<>();
    private static Map<String, ImprovedTimezone> TIMEZONES_BY_GMT = new HashMap<>();
    private static Map<String, ImprovedTimezone> TIMEZONES_BY_JNAME = new HashMap<>();
    private static final String TZ_FILE = StringUtil.packagePath(ImprovedTimezone.class) +"/timezones.txt";

    private static TimeZone SYSTEM_TIMEZONE;
    static {
        try {
            init();
        } catch (IOException e) {
            String msg = "Error initializing ImprovedTimezone from timezones.txt: "+e;
            log.error(msg, e);
            die(msg, e);
        }
        final TimeZone sysTimezone = TimeZone.getDefault();
        ImprovedTimezone tz = TIMEZONES_BY_JNAME.get(sysTimezone.getDisplayName());
        if (tz == null) {
            for (String displayName: TIMEZONES_BY_JNAME.keySet()) {
                ImprovedTimezone tz1 = TIMEZONES_BY_JNAME.get(displayName);
                String dn = displayName.replace("GMT-0","GMT-");
                dn = dn.replace("GMT+0", "GMT+");
                if (tz1.getGmtOffset().equals(dn)) {
                    tz = tz1;
                    break;
                }
            }
        }
        if (tz == null) {
            throw new ExceptionInInitializerError("System Timezone could not be located in timezones.txt");
        }

        SYSTEM_TIMEZONE = tz.getTimezone();
        log.info("System Time Zone set to " + SYSTEM_TIMEZONE.getDisplayName());
    }

    private ImprovedTimezone (int id,
                              String gmtOffset,
                              TimeZone timezone,
                              String displayName,
                              String linuxName) {
        this.id = id;
        this.gmtOffset = gmtOffset;
        this.timezone = timezone;
        this.displayName = displayName;
        this.linuxName = (linuxName == null) ? timezone.getDisplayName() : linuxName;
        this.displayNameWithOffset = "("+gmtOffset+") "+displayName;
    }

    public long getLocalTime (long systemTime) {
        // convert time to GMT
        final long gmtTime = systemTime - SYSTEM_TIMEZONE.getRawOffset();

        // now that we're in GMT, convert to local
        return gmtTime + getTimezone().getRawOffset();
    }

    public String toString () {
        return "[ImprovedTimezone id="+id+" offset="+gmtOffset
                +" name="+displayName+" zone="+timezone.getDisplayName() +"]";
    }

    public static List<ImprovedTimezone> getTimeZones () {
        return TIMEZONES;
    }

    public static ImprovedTimezone getTimeZoneById (int id) {
        final ImprovedTimezone tz = TIMEZONES_BY_ID.get(id);
        if (tz == null) {
            throw new IllegalArgumentException("Invalid timezone id: "+id);
        }
        return tz;
    }

    public static ImprovedTimezone getTimeZoneByJavaDisplayName (String name) {
        final ImprovedTimezone tz = TIMEZONES_BY_JNAME.get(name);
        if (tz == null) {
            throw new IllegalArgumentException("Invalid timezone name: "+name);
        }
        return tz;
    }

    public static ImprovedTimezone getTimeZoneByGmtOffset(String value) {
        return TIMEZONES_BY_GMT.get(value);
    }

    /**
     * Initialize timezones from a file on classpath.
     * The first line of the file is a header that is ignored.
     */
    private static void init () throws IOException {

        TIMEZONES = new ArrayList<>();
        try (InputStream in = StreamUtil.loadResourceAsStream(TZ_FILE)) {
            if (in == null) {
                throw new IOException("Error loading timezone file from classpath: "+TZ_FILE);
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
                String line = r.readLine();
                while (line != null) {
                    line = r.readLine();
                    if (line == null) break;
                    final ImprovedTimezone improvedTimezone = initZone(line);
                    TIMEZONES.add(improvedTimezone);
                    TIMEZONES_BY_ID.put(improvedTimezone.getId(), improvedTimezone);
                    TIMEZONES_BY_JNAME.put(improvedTimezone.getTimezone().getDisplayName(), improvedTimezone);
                    TIMEZONES_BY_GMT.put(improvedTimezone.getGmtOffset(), improvedTimezone);
                }
            }
        }
    }
    private static ImprovedTimezone initZone (String line) {
        try {
            final StringTokenizer st = new StringTokenizer(line, "|");
            int id = Integer.parseInt(st.nextToken());
            final String gmtOffset = st.nextToken();
            final String timezoneName = st.nextToken();
            final String displayName = st.nextToken();
            final String linuxName = st.hasMoreTokens() ? st.nextToken() : timezoneName;
            final TimeZone tz = TimeZone.getTimeZone(timezoneName);
            if (!gmtOffset.equals("GMT") && isGMT(tz)) {
                String msg = "Error looking up timezone: " + timezoneName + ": got GMT, expected " + gmtOffset;
                log.error(msg);
                die(msg);
            }
            return new ImprovedTimezone(id, gmtOffset, tz, displayName, linuxName);

        } catch (Exception e) {
            return die("Error processing line: "+line+": "+e, e);
        }
    }

    private static boolean isGMT(TimeZone tz) {
        return tz.getRawOffset() == 0;
    }

}
