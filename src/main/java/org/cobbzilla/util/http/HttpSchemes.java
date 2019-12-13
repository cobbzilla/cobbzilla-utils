package org.cobbzilla.util.http;

public enum  HttpSchemes {

    http, https;

    public static HttpSchemes from(String s) {
        return valueOf(s.toLowerCase());
    }

    public static boolean isValid(String s) {
        s = s.toLowerCase();
        for (HttpSchemes scheme : values()) {
            if (s.startsWith(scheme.name())) return true;
        }
        return false;
    }
}
