package org.cobbzilla.util.dns;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DnsServerType {

    dyn, namecheap, djbdns, bind;

    @JsonCreator public static DnsServerType create (String v) { return valueOf(v.toLowerCase()); }

}
