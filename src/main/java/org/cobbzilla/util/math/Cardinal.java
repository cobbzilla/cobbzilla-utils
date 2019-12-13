package org.cobbzilla.util.math;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

public enum Cardinal {

    north (1, "N", "north"),
    east (1, "E", "east"),
    south (-1, "S", "south"),
    west (-1, "W", "west");

    @Getter private final int direction;
    @Getter private final String[] allAliases;

    Cardinal(int direction, String... allAliases) {
        this.direction = direction;
        this.allAliases = allAliases;
    }

    @JsonCreator public static Cardinal create (String val) {
        for (Cardinal c : values()) {
            for (String a : c.allAliases) {
                if (a.equalsIgnoreCase(val)) return c;
            }
        }
        return null;
    }

    @Override public String toString () { return allAliases[0]; }

    public static boolean isCardinal(String val) {
        try {
            return create(val.toLowerCase()) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
