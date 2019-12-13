package org.cobbzilla.util.string;

import org.cobbzilla.util.collection.MapBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cobbzilla.util.string.StringUtil.chop;

public class ValidationRegexes {

    public static final Pattern LOGIN_PATTERN = pattern("^[\\w\\-]+$");
    public static final Pattern EMAIL_PATTERN = pattern("^[A-Z0-9][A-Z0-9._%+-]*@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
    public static final Pattern EMAIL_NAME_PATTERN = pattern("^[A-Z0-9][A-Z0-9._%+-]*$");

    public static final Pattern[] LOCALE_PATTERNS = {
            pattern("^[a-zA-Z]{2,3}([-_][a-zA-z]{2}(@[\\w]+)?)?"), // ubuntu style: en_US or just en
            pattern("^[a-zA-Z]{2,3}([-_][\\w]+)?"),             // some apps use style: ca-valencia
    };

    public static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    public static final Pattern UUID_PATTERN = pattern(UUID_REGEX);

    public static final String NUMERIC_REGEX = "\\d+";
    public static final Pattern NUMERIC_PATTERN = pattern(NUMERIC_REGEX);

    public static final int IP4_MAXLEN = 15;
    public static final int IP6_MAXLEN = 45;

    public static final Pattern IPv4_PATTERN = pattern("^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$");
    public static final Pattern IPv6_PATTERN = pattern("^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))$");

    public static final String HOST_REGEX = "^([A-Z0-9]{1,63}|[A-Z0-9][A-Z0-9\\-]{0,61}[A-Z0-9])(\\.([A-Z0-9]{1,63}|[A-Z0-9][A-Z0-9\\-]{0,61}[A-Z0-9]))*$";
    public static final Pattern HOST_PATTERN  = pattern(HOST_REGEX);
    public static final Pattern HOST_PART_PATTERN  = pattern("^([A-Z0-9]|[A-Z0-9][A-Z0-9\\-]{0,61}[A-Z0-9])$");
    public static final Pattern PORT_PATTERN  = pattern("^[\\d]{1,5}$");

    public static final String DOMAIN_REGEX = "^([A-Z0-9]{1,63}|[A-Z0-9][A-Z0-9\\-]{0,61}[A-Z0-9])(\\.([A-Z0-9]{1,63}|[A-Z0-9][A-Z0-9\\-]{0,61}[A-Z0-9]))+$";
    public static final Pattern DOMAIN_PATTERN  = pattern(DOMAIN_REGEX);

    public static final String URL_REGEX = "(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    public static final String URL_REGEX_ONLY = "^" + URL_REGEX + "$";
    public static final Pattern URL_PATTERN   = pattern(URL_REGEX_ONLY);
    public static final Pattern HTTP_PATTERN  = pattern("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$");
    public static final Pattern HTTPS_PATTERN = pattern("^https://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$");

    public static final String VARNAME_REGEX = "^[A-Za-z][_A-Za-z0-9]+$";
    public static final Pattern VARNAME_PATTERN = pattern(VARNAME_REGEX);

    public static final Pattern FILENAME_PATTERN = pattern("^[_A-Z0-9\\-\\.]+$");
    public static final Pattern INTEGER_PATTERN = pattern("^[0-9]+$");
    public static final Pattern DECIMAL_PATTERN = pattern("^[0-9]+(\\.[0-9]+)?$");

    public static final Map<String, Pattern> PHONE_PATTERNS = MapBuilder.build(new Object[][]{
            {"US", pattern("^\\d{10}$")}
    });
    public static final Pattern DEFAULT_PHONE_PATTERN = pattern("^\\d+([-\\.\\s]?\\d+?){8,}[\\d]+$");

    public static final String YYYYMMDD_REGEX = "^(19|20|21)[0-9]{2}-[01][0-9]-(0[1-9]|[1-2][0-9]|3[0-1])$";
    public static final Pattern YYYYMMDD_PATTERN = pattern(YYYYMMDD_REGEX);

    public static final String ZIPCODE_REGEX = "^\\d{5}(-\\d{4})?$";
    public static final Pattern ZIPCODE_PATTERN = pattern(ZIPCODE_REGEX);

    public static Pattern pattern(String regex) { return Pattern.compile(regex, Pattern.CASE_INSENSITIVE); }

    public static List<String> findAllRegexMatches(String text, String regex) {
        if (regex.startsWith("^")) regex = regex.substring(1);
        if (regex.endsWith("$")) regex = chop(regex, "$");
        regex = "(.*(?<match>"+ regex +")+.*)+";
        final List<String> found = new ArrayList<>();
        final Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(text);
        while (matcher.find()) {
            found.add(matcher.group("match"));
        }
        return found;
    }

}
