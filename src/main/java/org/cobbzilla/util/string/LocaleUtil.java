package org.cobbzilla.util.string;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.string.StringUtil.getPackagePath;

@Slf4j
public class LocaleUtil {

    public static final String ISO_COUNTRY_CODES_RESOURCE = getPackagePath(LocaleUtil.class) + "/country_codes.txt";

    public static final Set<String> ISO_COUNTRY_CODES = Arrays.stream(stream2string(ISO_COUNTRY_CODES_RESOURCE).split("\n"))
            .filter(cc -> !empty(cc) && !cc.trim().startsWith("#"))
            .map(cc -> cc.trim().toLowerCase())
            .collect(Collectors.toSet());

    public static boolean isValidCountryCode (String cc) {
        return ISO_COUNTRY_CODES.contains(cc.toLowerCase());
    }

    public static final String TELEPHONE_CODES_RESOURCE = getPackagePath(LocaleUtil.class) + "/telephone_codes.txt";
    private static final Properties TELEPHONE_CODES_MAP = initPhoneCodes();
    private static Properties initPhoneCodes() {
        try {
            final Properties props = new Properties();
            props.load(new StringReader(stream2string(TELEPHONE_CODES_RESOURCE)));
            return props;
        } catch (Exception e) {
            return die("initPhoneCodes: "+e, e);
        }
    }

    public static String getPhoneCode (String country) {
        if (country == null) return null;
        return TELEPHONE_CODES_MAP.getProperty(country.toUpperCase());
    }

    public static File findLocaleFile (File base, String locale) {

        if (empty(locale)) return base.exists() ? base : null;

        final String[] localeParts = locale.toLowerCase().replace("-", "_").split("_");
        final String lang = localeParts[0];
        final String region = localeParts.length > 1 ? localeParts[1] : null;
        final String variant = localeParts.length > 2 ? localeParts[2] : null;

        File found;
        if (!empty(variant)) {
            found = findSpecificLocaleFile(base, lang + "_" + region + "_" + variant);
            if (found != null) return found;
        }
        if (!empty(region)) {
            found = findSpecificLocaleFile(base, lang + "_" + region);
            if (found != null) return found;
        }
        found = findSpecificLocaleFile(base, lang);
        if (found != null) return found;

        return base.exists() ? base : null;
    }

    private static File findSpecificLocaleFile(File base, String locale) {
        final String filename = base.getName();
        final int lastDot = filename.lastIndexOf('.');
        final String prefix;
        final String suffix;
        if (lastDot != -1) {
            prefix = filename.substring(0, lastDot);
            suffix = filename.substring(lastDot);
        } else {
            prefix = filename;
            suffix = "";
        }
        final File localeFile = new File(base.getParent(), prefix + "_" + locale + suffix);
        return localeFile.exists() ? localeFile : null;
    }

    public static Locale fromString(String localeString) {
        final String[] parts = empty(localeString) ? StringUtil.EMPTY_ARRAY : localeString.split("[-_]+");
        switch (parts.length) {
            case 3: return new Locale(parts[0], parts[1], parts[2]);
            case 2: return new Locale(parts[0], parts[1]);
            case 1: return new Locale(parts[0]);
            case 0: return Locale.getDefault();
            default:
                log.warn("fromString: invalid locale string: "+localeString);
                return Locale.getDefault();
        }
    }

    @Getter(lazy=true) private static final Map<String, List<String>> defaultLocales = initDefaultLocales();
    private static Map<String, List<String>> initDefaultLocales() {
        final Map<String, List<String>> defaults = new HashMap<>();
        final JsonNode node = json(stream2string(getPackagePath(LocaleUtil.class)+"/default_locales.json"), JsonNode.class);
        return buildDefaultsMap(defaults, node);
    }
    public static List<String> getDefaultLocales(String country) { return getDefaultLocales().get(country); }

    @Getter(lazy=true) private static final Map<String, List<String>> defaultLanguages = initDefaultLanguages();
    private static Map<String, List<String>> initDefaultLanguages() {
        final Map<String, List<String>> defaults = new HashMap<>();
        final JsonNode node = json(stream2string(getPackagePath(LocaleUtil.class)+"/default_langs.json"), JsonNode.class);
        return buildDefaultsMap(defaults, node);
    }
    public static List<String> getDefaultLanguages(String country) { return getDefaultLanguages().get(country); }

    private static Map<String, List<String>> buildDefaultsMap(Map<String, List<String>> defaults, JsonNode node) {
        for (Iterator<String> iter = node.fieldNames(); iter.hasNext(); ) {
            final String country = iter.next();
            final JsonNode countryNode = node.get(country);
            final List<String> locales = new ArrayList<>();
            for (Iterator<JsonNode> iter2 = countryNode.iterator(); iter2.hasNext(); ) {
                locales.add(iter2.next().textValue());
            }
            defaults.put(country, locales);
        }
        return defaults;
    }
}
