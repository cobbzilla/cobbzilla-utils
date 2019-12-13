package org.cobbzilla.util.dns;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DnsType {

    A, AAAA, CNAME, MX, NS, TXT, SOA, PTR,     // very common record types
    RP, LOC, SIG, SPF, SRV, TSIG, TKEY, CERT,  // sometimes used
    KEY, DS, DNSKEY, NSEC, NSEC3, NSEC3PARAM, RRSIG, IPSECKEY, DLV, // DNSSEC and other security-related types
    DNAME, DLCID, HIP, NAPTR, SSHFP, TLSA,     // infrequently used
    IXFR, AXFR, OPT;                           // pseudo-record types

    public static final DnsType[] A_TYPES = new DnsType[] {A, AAAA};

    @JsonCreator public static DnsType fromString(String value) { return valueOf(value.toUpperCase()); }

}
