package org.cobbzilla.util.dns;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.StringUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Comparator.comparing;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.dns.DnsType.A;
import static org.cobbzilla.util.dns.DnsType.SOA;

@NoArgsConstructor @Accessors(chain=true) @ToString(callSuper=true)
public class DnsRecord extends DnsRecordBase {

    public static final int DEFAULT_TTL = (int) TimeUnit.HOURS.toSeconds(1);

    public static final String OPT_MX_RANK = "rank";
    public static final String OPT_NS_NAME = "ns";

    public static final String OPT_SOA_MNAME = "mname";
    public static final String OPT_SOA_RNAME = "rname";
    public static final String OPT_SOA_SERIAL = "serial";
    public static final String OPT_SOA_REFRESH = "refresh";
    public static final String OPT_SOA_RETRY = "retry";
    public static final String OPT_SOA_EXPIRE = "expire";
    public static final String OPT_SOA_MINIMUM = "minimum";

    public static final String[] MX_REQUIRED_OPTIONS = {OPT_MX_RANK};
    public static final String[] NS_REQUIRED_OPTIONS = {OPT_NS_NAME};
    public static final String[] SOA_REQUIRED_OPTIONS = {
            OPT_SOA_MNAME, OPT_SOA_RNAME, OPT_SOA_SERIAL, OPT_SOA_REFRESH, OPT_SOA_EXPIRE, OPT_SOA_RETRY
    };

    public static final Comparator<? super DnsRecord> DUPE_COMPARATOR = comparing(DnsRecord::dnsUniq);

    @Getter @Setter private int ttl = DEFAULT_TTL;
    @Getter @Setter private Map<String, String> options;
    public boolean hasOptions () { return options != null && !options.isEmpty(); }

    public DnsRecord (DnsType type, String fqdn, String value, int ttl) {
        setType(type);
        setFqdn(fqdn);
        setValue(value);
        setTtl(ttl);
    }

    public static DnsRecord A(String host, String ip) {
        return (DnsRecord) new DnsRecord().setType(A).setFqdn(host).setValue(ip);
    }

    public DnsRecord setOption(String optName, String value) {
        if (options == null) options = new HashMap<>();
        options.put(optName, value);
        return this;
    }

    public String getOption(String optName) { return options == null ? null : options.get(optName); }

    public int getIntOption(String optName, int defaultValue) {
        try {
            return Integer.parseInt(options.get(optName));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    @JsonIgnore public String[] getRequiredOptions () {
        switch (getType()) {
            case MX: return MX_REQUIRED_OPTIONS;
            case NS: return NS_REQUIRED_OPTIONS;
            case SOA: return SOA_REQUIRED_OPTIONS;
            default: return StringUtil.EMPTY_ARRAY;
        }
    }

    @JsonIgnore public boolean hasAllRequiredOptions () {
        for (String opt : getRequiredOptions()) {
            if (options == null || !options.containsKey(opt)) return false;
        }
        return true;
    }

    public String getOptions_string(String sep) {
        final StringBuilder b = new StringBuilder();
        if (options != null) {
            for (Map.Entry<String, String> e : options.entrySet()) {
                if (b.length() > 0) b.append(sep);
                if (empty(e.getValue())) {
                    b.append(e.getKey()).append("=true");
                } else {
                    b.append(e.getKey()).append("=").append(e.getValue());
                }
            }
        }
        return b.toString();
    }

    public DnsRecord setOptions_string(String arg) {
        if (options == null) options = new HashMap<>();
        if (empty(arg)) return this;

        for (String kvPair : arg.split(",")) {
            int eqPos = kvPair.indexOf("=");
            if (eqPos == kvPair.length()) throw new IllegalArgumentException("Option cannot end in '=' character");
            if (eqPos == -1) {
                options.put(kvPair.trim(), "true");
            } else {
                options.put(kvPair.substring(0, eqPos).trim(), kvPair.substring(eqPos+1).trim());
            }
        }
        return this;
    }

    public String dnsUniq() { return type == SOA ? SOA+":"+fqdn : dnsFormat(",", "|"); }

    public String dnsFormat() {
        return dnsFormat(",", "|");
    }
    public String dnsFormat(String fieldSep, String optionsSep) {
        return getType().name().toUpperCase()+fieldSep+getFqdn()+fieldSep+getValue()+fieldSep+getTtl()+fieldSep+(!hasOptions() ? "" : getOptions_string(optionsSep));
    }
}
