package org.cobbzilla.util.json;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum JsonEditOperationType {

    read, write, delete, sort;

    @JsonCreator public static JsonEditOperationType create(String value) { return valueOf(value.toLowerCase()); }

}
