package org.cobbzilla.util.network;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum NetworkInterfaceType {

    local, world, vpn, vpn2, custom;

    @JsonCreator public static NetworkInterfaceType create (String v) { return valueOf(v.toLowerCase()); }

}
