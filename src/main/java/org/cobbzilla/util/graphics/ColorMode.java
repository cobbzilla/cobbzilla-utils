package org.cobbzilla.util.graphics;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ColorMode {

    rgb, ansi;

    @JsonCreator public static ColorMode fromString (String val) { return valueOf(val.toLowerCase()); }

}
