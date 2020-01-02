package org.cobbzilla.util.dns;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Set;
import java.util.regex.Pattern;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @Accessors(chain=true) @ToString(of={"pattern", "fqdns", "subdomain"}, callSuper=true)
public class DnsRecordMatch extends DnsRecordBase {

    @Getter @Setter private String pattern;
    @JsonIgnore @Getter(lazy=true) private final Pattern _pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

    @Getter @Setter private Set<String> fqdns;
    public boolean hasFqdns () { return fqdns != null && !fqdns.isEmpty(); }

    public DnsRecordMatch(Set<String> fqdns) { this.fqdns = fqdns; }

    public boolean hasPattern() { return !empty(pattern); }

    @Getter @Setter private String subdomain;
    public boolean hasSubdomain() { return !empty(subdomain); }

    public DnsRecordMatch(DnsRecordBase record) {
        super(record.getFqdn(), record.getType(), record.getValue());
    }

    public DnsRecordMatch(DnsType type, String fqdn) {
        setType(type);
        setFqdn(fqdn);
    }

    public DnsRecordMatch(String fqdn) { this(null, fqdn); }

    public static DnsRecordMatch invert(DnsRecordMatch other) {
        return new DnsRecordMatch() {
            @Override public boolean matches(DnsRecord record) {
                return !other.matches(record);
            }
        };
    }

    public boolean matches (DnsRecord record) {
        if (hasType() && !getType().equals(record.getType())) return false;
        if (hasFqdn() && !getFqdn().equalsIgnoreCase(record.getFqdn())) return false;
        if (hasSubdomain() && record.hasFqdn() && !record.getFqdn().toLowerCase().endsWith(getSubdomain().toLowerCase())) return false;
        if (hasPattern() && record.hasFqdn() && !get_pattern().matcher(record.getFqdn()).find()) return false;
        if (hasFqdns() && record.hasFqdn() && getFqdns().stream().noneMatch(f -> record.getFqdn().equalsIgnoreCase(f))) return false;
        return true;
    }

}
