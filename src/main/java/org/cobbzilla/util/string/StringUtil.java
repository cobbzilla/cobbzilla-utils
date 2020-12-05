package org.cobbzilla.util.string;

import com.google.common.base.CaseFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.cobbzilla.util.javascript.JsEngine;
import org.cobbzilla.util.javascript.JsEngineConfig;
import org.cobbzilla.util.security.MD5Util;
import org.cobbzilla.util.time.JavaTimezone;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringEscapeUtils.escapeSql;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.collection.ArrayUtil.arrayToString;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;

public class StringUtil {

    public static final String UTF8 = "UTF-8";
    public static final Charset UTF8cs = StandardCharsets.UTF_8;

    public static final String EMPTY = "";
    public static final String[] EMPTY_ARRAY = {};
    public static final char[] EMPTY_CHAR_ARRAY = {};
    public static final String CRLF = "\r\n";

    public static final Transformer XFORM_TO_STRING = o -> String.valueOf(o);

    public static final String[] VOWELS = {"e", "a", "o", "i", "u"};

    public static boolean isVowel(String symbol) { return ArrayUtils.indexOf(VOWELS, symbol) != -1; }

    public static List<String> toStringCollection (Collection c) {
        return new ArrayList<>(CollectionUtils.collect(c, XFORM_TO_STRING));
    }

    public static String prefix(String s, int count) {
        return s == null ? null : s.length() > count ? s.substring(0, count) : s;
    }

    public static String packagePath(Class clazz) {
        return clazz.getPackage().getName().replace(".","/");
    }

    public static String packagePath(String clazz) { return clazz.replace(".","/"); }

    public static List<String> split (String s, String delim) {
        final StringTokenizer st = new StringTokenizer(s, delim);
        final List<String> results = new ArrayList<>();
        while (st.hasMoreTokens()) {
            results.add(st.nextToken());
        }
        return results;
    }

    public static String[] split2array (String s, String delim) {
        final List<String> vals = split(s, delim);
        return vals.toArray(new String[vals.size()]);
    }

    public static List<Long> splitLongs (String s, String delim) {
        final StringTokenizer st = new StringTokenizer(s, delim);
        final List<Long> results = new ArrayList<>();
        while (st.hasMoreTokens()) {
            final String token = st.nextToken();
            results.add(empty(token) || token.equalsIgnoreCase("null") ? null : Long.parseLong(token));
        }
        return results;
    }

    public static List<String> splitAndTrim (String s, String delim) {
        final List<String> results = new ArrayList<>();
        if (empty(s)) return results;
        final StringTokenizer st = new StringTokenizer(s, delim);
        while (st.hasMoreTokens()) {
            results.add(st.nextToken().trim());
        }
        return results;
    }

    public static String replaceLast(String s, String find, String replace) {
        if (empty(s)) return s;
        int lastIndex = s.lastIndexOf(find);
        if (lastIndex < 0) return s;
        return s.substring(0, lastIndex) + s.substring(lastIndex).replaceFirst(find, replace);
    }

    public static String lastPathElement(String url) { return url.substring(url.lastIndexOf("/")+1); }

    public static String safeShellArg (String s) { return s.replaceAll("[^-\\._ \t/=\\w]+", ""); }
    public static boolean checkSafeShellArg (String s) { return safeShellArg(s).equals(s); }
    public static String safeFunctionName (String s) { return s.replaceAll("\\W", ""); }
    public static String safeSnakeName (String s) { return s.replaceAll("\\W", "_"); }

    public static String onlyDigits (String s) { return s.replaceAll("\\D+", ""); }

    public static String removeWhitespace (String s) { return s.replaceAll("\\p{javaSpaceChar}", ""); }

    public static Integer safeParseInt(String s) {
        if (empty(s)) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double safeParseDouble(String s) {
        if (empty(s)) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String shortDateTime(String localeString, String timezone, long time) {
        return formatDateTime("SS", localeString, timezone, time);
    }

    public static String mediumDateTime(String localeString, String timezone, long time) {
        return formatDateTime("MM", localeString, timezone, time);
    }

    public static String fullDateTime(String localeString, String timezone, long time) {
        return formatDateTime("FF", localeString, timezone, time);
    }

    public static String formatDateTime(String style, String localeString, String timezone, long time) {
        final Locale locale = LocaleUtils.toLocale(localeString);
        final JavaTimezone tz = JavaTimezone.fromString(timezone);
        return DateTimeFormat.forPattern(DateTimeFormat.patternForStyle(style, locale))
                .withZone(DateTimeZone.forTimeZone(tz.getTimeZone())).print(time);
    }

    public static final long HOUR = TimeUnit.HOURS.toMillis(1);
    public static final long MINUTE = TimeUnit.MINUTES.toMillis(1);
    public static final long SECOND = TimeUnit.SECONDS.toMillis(1);

    public static String chopSuffix(String val) { return val.substring(0, val.length()-1); }

    public static String chopToFirst(String val, String find) {
        return !val.contains(find) ? val : val.substring(val.indexOf(find) + find.length());
    }

    public static String trimQuotes (String s) {
        if (s == null) return s;
        while (s.startsWith("\"") || s.startsWith("\'")) s = s.substring(1);
        while (s.endsWith("\"") || s.endsWith("\'")) s = s.substring(0, s.length()-1);
        return s;
    }

    public static boolean endsWithAny(String s, String[] suffixes) {
        if (s == null) return false;
        for (String suffix : suffixes) if (s.endsWith(suffix)) return true;
        return false;
    }

    public static String getPackagePath(Class clazz) {
        return clazz.getPackage().getName().replace('.', '/');
    }

    public static String repeat (String s, int n) {
        return new String(new char[n*s.length()]).replace("\0", s);
    }

    public static String urlEncode (String s) {
        try {
            return URLEncoder.encode(s, UTF8);
        } catch (UnsupportedEncodingException e) {
            return die("urlEncode: "+e, e);
        }
    }

    public static String simpleUrlEncode (String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static String urlDecode (String s) {
        try {
            return URLDecoder.decode(s, UTF8);
        } catch (UnsupportedEncodingException e) {
            return die("urlDecode: "+e, e);
        }
    }

    public static URI uriOrDie (String s) {
        try {
            return new URI(s);
        } catch (URISyntaxException e) {
            return die("bad uri: "+e, e);
        }
    }

    public static String urlParameterize(Map<String, String> params) {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(urlEncode(entry.getKey()))
                    .append('=')
                    .append(urlEncode(entry.getValue()));
        }
        return sb.toString();
    }

    public static String toString (Collection c) { return toString(c, ","); }

    public static String toString (Collection c, String sep) {
        return toString(c, sep, null);
    }

    public static String toString (Collection c, String sep, Function<Object, String> transformer) {
        StringBuilder builder = new StringBuilder();
        for (Object o : c) {
            if (builder.length() > 0) builder.append(sep);
            builder.append(transformer != null ? transformer.apply(o) : o);
        }
        return builder.toString();
    }

    public static String sqlIn (Collection c) { return toString(c, ",", ESCAPE_SQL); }

    public static final Function<Object, String> ESCAPE_SQL = o -> "'"+o.toString().replace("\'", "\'\'")+"'";

    public static String toString(Map map) {
        if (map == null) return "null";
        final StringBuilder b = new StringBuilder("{");
        for (Object key : map.keySet()) {
            final Object value = map.get(key);
            b.append(key).append("=");
            if (value == null) {
                b.append("null");
            } else {
                if (value.getClass().isArray()) {
                    b.append(arrayToString((Object[]) value, ", "));
                } else if (value instanceof Map) {
                    b.append(toString((Map) value));
                } else if (value instanceof Collection) {
                    b.append(toString((Collection) value, ", "));
                } else {
                    b.append(value);
                }
            }
        }
        return b.append("}").toString();
    }

    public static Set<String> toSet (String s, String sep) {
        return new HashSet<>(Arrays.asList(s.split(sep)));
    }

    public static String tohex(byte[] data) {
        return tohex(data, 0, data.length);
    }

    public static String tohex(byte[] data, int start, int len) {
        StringBuilder b = new StringBuilder();
        int stop = start+len;
        for (int i=start; i<stop; i++) {
            b.append(getHexValue(data[i]));
        }
        return b.toString();
    }

    public static String tohexArray(byte[] data) {
        return tohexArray(data, 0, data.length, ", ");
    }
    public static String tohexArray(byte[] data, int start, int len, String delim) {
        StringBuilder b = new StringBuilder();
        int stop = start+len;
        for (int i=start; i<stop; i++) {
            if (b.length() > 0) b.append(delim);
            b.append("0x").append(getHexValue(data[i]));
        }
        return b.toString();
    }

    /**
     * Get the hexadecimal string representation for a byte.
     * The leading 0x is not included.
     *
     * @param b the byte to process
     * @return a String representing the hexadecimal value of the byte
     */
    public static String getHexValue(byte b) {
        int i = (int) b;
        return MD5Util.HEX_DIGITS[((i >> 4) + 16) % 16] + MD5Util.HEX_DIGITS[(i + 128) % 16];
    }

    public static String uncapitalize(String s) {
        return empty(s) ? s : s.length() == 1 ? s.toLowerCase() : s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    public static String pluralize(String val) {
        if (empty(val)) return val;
        if (val.endsWith("y")) {
            return val.substring(0, val.length()-1)+"ies";
        } else if (!val.endsWith("s")) {
            return val + "s";
        }
        return val;
    }

    public static boolean exceptionContainsMessage(Throwable e, String s) {
        return e != null && (
                (e.getMessage() != null && e.getMessage().contains(s))
                        || (e.getCause() != null && exceptionContainsMessage(e.getCause(), s))
        );
    }

    public static String ellipsis(String s, int len) {
        if (s == null || s.length() <= len) return s;
        return s.substring(0, len-3) + "...";
    }

    public static String truncate(String s, int len) {
        if (s == null || s.length() <= len) return s;
        return s.substring(0, len);
    }

    public static boolean containsIgnoreCase(Collection<String> values, String value) {
        for (String v : values) if (v != null && v.equalsIgnoreCase(value)) return true;
        return false;
    }

    /**
     * Return what the default "property name" would be for this thing, if named according to its type
     * @param thing the thing to look at
     * @param <T> the type of thing it is
     * @return the class name of the thing with the first letter downcased
     */
    public static <T> String classAsFieldName(T thing) {
        return uncapitalize(thing.getClass().getSimpleName());
    }

    /**
     * Split a string into multiple query terms, respecting quotation marks
     * @param query The query string
     * @return a List of query terms
     */
    public static List<String> splitIntoTerms(String query) {
        final List<String> terms = new ArrayList<>();
        final StringTokenizer st = new StringTokenizer(query, "\n\t \"", true);

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        while (st.hasMoreTokens()) {
            final String token = st.nextToken();
            if (token.equals("\"")) {
                String term = current.toString().trim();
                if (term.length() > 0) terms.add(term);
                current = new StringBuilder();
                inQuotes = !inQuotes;

            } else if (token.matches("\\s+")) {
                if (inQuotes && !current.toString().endsWith(" ")) current.append(" ");

            } else {
                if (inQuotes) {
                    current.append(token);
                } else {
                    terms.add(token);
                }
            }
        }
        if (current.length() > 0) terms.add(current.toString().trim());
        return terms;
    }

    public static String chop(String input, String chopIfSuffix) {
        return input.endsWith(chopIfSuffix) ? input.substring(0, input.length()-chopIfSuffix.length()) : input;
    }

    public static boolean isNumber(String val) {
        if (val == null) return false;
        val = val.trim();
        try {
            Double.parseDouble(val);
            return true;
        } catch (Exception ignored) {}
        try {
            Long.parseLong(val);
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean isPunctuation(char c) {
        return c == '.' || c == ',' || c == '?' || c == '!' || c == ';' || c == ':';
    }

    public static boolean hasScripting(String value) {
        if (empty(value)) return false;
        value = value.toLowerCase().replace("&lt;", "<");
        final String nospace = removeWhitespace(value);
        return nospace.contains("<script") || nospace.contains("javascript:")
                || (nospace.contains("onfocus=") && value.contains(" onfocus"))
                || (nospace.contains("onblur=") && value.contains(" onblur"))
                || (nospace.contains("onload=") && value.contains(" onload"))
                || (nospace.contains("onunload=") && value.contains(" onunload"))
                || (nospace.contains("onselect=") && value.contains(" onselect"))
                || (nospace.contains("onchange=") && value.contains(" onchange"))
                || (nospace.contains("onmove=") && value.contains(" onmove"))
                || (nospace.contains("onreset=") && value.contains(" onreset"))
                || (nospace.contains("onresize=") && value.contains(" onresize"))
                || (nospace.contains("onclick=") && value.contains(" onclick"))
                || (nospace.contains("ondblclick=") && value.contains(" ondblclick"))
                || (nospace.contains("onmouseup=") && value.contains(" onmouseup"))
                || (nospace.contains("onmousedown=") && value.contains(" onmousedown"))
                || (nospace.contains("onmouseout=") && value.contains(" onmouseout"))
                || (nospace.contains("onmouseover=") && value.contains(" onmouseover"))
                || (nospace.contains("onmousemove=") && value.contains(" onmousemove"))
                || (nospace.contains("ondragdrop=") && value.contains(" ondragdrop"))
                || (nospace.contains("onkeyup=") && value.contains(" onkeyup"))
                || (nospace.contains("onkeydown=") && value.contains(" onkeydown"))
                || (nospace.contains("onkeypress=") && value.contains(" onkeypress"))
                || (nospace.contains("onsubmit=") && value.contains(" onsubmit"))
                || (nospace.contains("onerror=") && value.contains(" onerror"));
    }

    public static String camelCaseToString(String val) {
        if (empty(val)) return val;
        final StringBuilder b = new StringBuilder();
        b.append(Character.toUpperCase(val.charAt(0)));
        if (val.length() == 1) return b.toString();
        for (int i=1; i<val.length(); i++) {
            char c = val.charAt(i);
            if (Character.isUpperCase(c)) {
                b.append(' ');
            }
            b.append(c);
        }
        return b.toString();
    }

    public static String snakeCaseToCamelCase(String snake) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, snake);
    }

    public static String camelCaseToSnakeCase(String camel) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, camel);
    }

    public static String formatCents(int cents) { return "" + (cents/100) + (cents % 100 == 0 ? "" : (cents % 100 > 10) ? "."+ (cents % 100) : ".0"+(cents % 100)); }

    public static String hexPath(String hex, int count) {
        final StringBuilder b = new StringBuilder();
        for (int i=0; i<count; i++) {
            if (b.length() > 0) b.append("/");
            b.append(hex.substring(i*2, i*2 + 2));
        }
        return b.toString();
    }

    public static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();
    public static String formatDollars(long value, boolean sign) { return empty(value) ? "" : (sign?"$":"") + NUMBER_FORMAT.format(value); }

    public static String formatDollars(long value)         { return formatDollars(value, true); }
    public static String formatDollarsWithSign(long value) { return formatDollars(value, true); }
    public static String formatDollarsNoSign(long value)   { return formatDollars(value, false); }

    public static String formatDollarsAndCents(long value)         { return formatDollarsAndCents(value, true); }
    public static String formatDollarsAndCentsWithSign(long value) { return formatDollarsAndCents(value, true); }
    public static String formatDollarsAndCentsNoSign(long value)   { return formatDollarsAndCents(value, false); }

    public static String formatDollarsAndCents(long value, boolean sign) {
        return empty(value) ? "" : (sign?"$":"") + NUMBER_FORMAT.format(value/100)
                + (value % 100 == 0 ? ".00" : "."+(value%100<10 ? "0"+(value%100) : (value%100)));
    }

    public static String formatDollarsAndCentsPlain(long value) {
        return "" + (value/100) + (value % 100 == 0 ? ".00" : "."+(value%100<10 ? "0"+(value%100) : (value%100)));
    }

    public static int parseToCents(String amount) {
        if (empty(amount)) return die("getDownAmountCents: downAmount was empty");
        String val = amount.trim();
        int dotPos = val.indexOf(".");
        if (dotPos == val.length()) {
            val = val.substring(0, val.length()-1);
            dotPos = -1;
        }
        if (dotPos == -1) return 100 * Integer.parseInt(val);
        return (100 * Integer.parseInt(val.substring(0, dotPos))) + Integer.parseInt(val.substring(dotPos+1));
    }

    public static double parsePercent (String pct) {
        if (empty(pct)) die("parsePercent: "+pct);
        return Double.parseDouble(chop(removeWhitespace(pct), "%"));
    }

    public static ReaderInputStream stream(String data) {
        return new ReaderInputStream(new StringReader(data), UTF8cs);
    }

    public static String firstMatch(String s, String regex) {
        final Pattern p = Pattern.compile(regex);
        final Matcher m = p.matcher(s);
        return m.find() ? m.group(0) : null;
    }

    private static final String DIFF_JS
            = loadResourceAsStringOrDie(getPackagePath(StringUtil.class)+"/diff_match_patch.js") + "\n"
            + loadResourceAsStringOrDie(getPackagePath(StringUtil.class)+"/calc_diff.js") + "\n";
    public static JsEngine DIFF_JS_ENGINE = new JsEngine(new JsEngineConfig(5, 20, null));
    public static String diff (String text1, String text2, Map<String, String> opts) {
        if (opts == null) opts = new HashMap<>();
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put("text1", text1);
        ctx.put("text2", text2);
        ctx.put("opts", opts);
        return DIFF_JS_ENGINE.evaluate(DIFF_JS, ctx);
    }

    public static String replaceWithRandom(String s, String find, int randLength) {
        while (s.contains(find)) s = s.replaceFirst(find, randomAlphanumeric(randLength));
        return s;
    }

    public static String firstWord(String value) { return value.trim().split("\\p{javaSpaceChar}+")[0]; }

    /**
     * If both strings are empty (null or empty string) return true, else use apache's StringUtils.equals method.
     */
    public static boolean equalsExtended(String s1, String s2) {
        return (empty(s1) && empty(s2)) || StringUtils.equals(s1, s2);
    }

    public static final String PCT = "%";
    public static final String ESC_PCT = "[%]";
    public static String sqlFilter(String value) {
        // escape any embedded '%' chars, and then add '%' as the first and last chars
        // also replace any embedded single-quote characters with '%', this helps prevent SQL injection attacks
        return PCT + value.toLowerCase().replace(PCT, ESC_PCT).replace("'", PCT) + PCT;
    }

    public static String sqlEscapeAndQuote(String val) { return "'" + escapeSql(val) + "'"; }

    public static List<Map<Integer, String>> findAllMatches(String val, String regex, Collection<Integer> groups) {
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(val);
        final List<Map<Integer, String>> matches = new ArrayList<>();
        while (matcher.find()) {
            final Map<Integer, String> match = new HashMap<>();
            if (groups == null) {
                match.put(0, matcher.group());
            } else {
                for (Integer group : groups) {
                    match.put(group, group == 0 ? matcher.group() : group > matcher.groupCount() ? null : matcher.group(group));
                }
            }
            matches.add(match);
        }
        return matches;
    }

}
