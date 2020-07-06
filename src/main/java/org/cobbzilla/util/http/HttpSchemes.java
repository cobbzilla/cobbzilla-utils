package org.cobbzilla.util.http;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum HttpSchemes {

    http, https, file;

    public static final String SCHEME_HTTP = http.schemeWithSeparator();
    public static final String SCHEME_HTTPS = https.schemeWithSeparator();
    public static final String SCHEME_FILE = file.schemeWithSeparator();
    public static final String PROTOCOL_SEP = "://";

    public String schemeWithSeparator() { return this.name()+PROTOCOL_SEP; }

    public static boolean isHttpOrHttps(String val) {
        return val != null && (val.startsWith(SCHEME_HTTP) || val.startsWith(SCHEME_HTTPS));
    }

    public static String stripScheme(String val) {
        if (val == null) return val;
        final int protoPos = val.indexOf(PROTOCOL_SEP);
        return protoPos == -1 ? val : val.substring(protoPos+PROTOCOL_SEP.length());
    }

    @JsonCreator public static HttpSchemes fromString (String s) { return valueOf(s.toLowerCase()); }

    public static boolean isValid(String s) {
        s = s.toLowerCase();
        for (HttpSchemes scheme : values()) {
            if (s.startsWith(scheme.name())) return true;
        }
        return false;
    }
}
