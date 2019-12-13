package org.cobbzilla.util.network;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransportProtocol {

    icmp, udp, tcp;

    @JsonCreator public static TransportProtocol create (String v) { return valueOf(v.toLowerCase()); }

}
