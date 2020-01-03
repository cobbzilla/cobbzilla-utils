package org.cobbzilla.util.dns;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
@ToString @EqualsAndHashCode @Slf4j
public class DnsRecordBase {

    @Getter @Setter protected String fqdn;
    public boolean hasFqdn() { return !empty(fqdn); }

    public static String dropTailingDot(String name) { return name.endsWith(".") ? name.substring(0, name.length()-1) : name; }

    public String getHost (String suffix) {
        if (!hasFqdn()) return die("getHost: fqdn not set");
        if (getFqdn().endsWith(suffix)) return getFqdn().substring(0, getFqdn().length() - suffix.length() - 1);
        log.warn("getHost: suffix mismatch: fqdn "+getFqdn()+" does not end with "+suffix);
        return getFqdn();
    }

    @Getter @Setter protected DnsType type;
    public boolean hasType () { return type != null; }

    @Getter @Setter protected String value;
    public boolean hasValue () { return !empty(value); }

    @JsonIgnore public DnsRecordMatch getMatcher() {
        return (DnsRecordMatch) new DnsRecordMatch().setFqdn(fqdn).setType(type).setValue(value);
    }

    @JsonIgnore public DnsRecordMatch getNonMatcher() { return DnsRecordMatch.invert(getMatcher()); }

}
