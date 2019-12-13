package org.cobbzilla.util.security;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum HashType {

    sha256;

    @JsonCreator public static HashType create(String value) { return valueOf(value.toLowerCase()); }
}
