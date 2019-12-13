package org.cobbzilla.util.jdbc;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DbDumpMode {

    all, schema, data, pre_data, post_data;

    @JsonCreator public static DbDumpMode fromString (String val) { return valueOf(val.toLowerCase()); }

}
