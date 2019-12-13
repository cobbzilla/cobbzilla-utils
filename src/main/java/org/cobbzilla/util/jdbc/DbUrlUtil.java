package org.cobbzilla.util.jdbc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DbUrlUtil {

    public static final Pattern JDBC_URL_REGEX = Pattern.compile("^jdbc:postgresql://[\\.\\w]+:\\d+/(.+)$");

    public static String setDbName(String url, String dbName) {
        final Matcher matcher = JDBC_URL_REGEX.matcher(url);
        if (!matcher.find()) return url;
        final String renamed = matcher.replaceFirst(dbName);
        return renamed;
    }

}
