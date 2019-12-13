package org.cobbzilla.util.time;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TimeRelativeType {

    past, present, future;

    @JsonCreator public static TimeRelativeType fromString (String val) { return valueOf(val.toLowerCase()); }

}
