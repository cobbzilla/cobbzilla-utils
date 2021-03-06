package org.cobbzilla.util.http;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.urlDecode;

@Slf4j
public class URIUtil {

    public static URI toUri(String uri) {
        try { return new URI(uri); } catch (URISyntaxException e) {
            return die("toUri: invalid URI: " + uri);
        }
    }

    public static URI toUriOrNull(String uri) {
        try { return new URI(uri); } catch (URISyntaxException e) {
            log.warn("toUriOrNull: invalid URI (returning null): " + uri);
            return null;
        }
    }

    public static String getScheme(String uri) { return toUri(uri).getScheme(); }
    public static String getHost(String uri) { return toUri(uri).getHost(); }
    public static int getPort(String uri) { return toUri(uri).getPort(); }
    public static String getPath(String uri) { return toUri(uri).getPath(); }

    public static String getHostUri(String uri) {
        final URI u = toUri(uri);
        return u.getScheme() + "://" + u.getHost();
    }

    /**
     * getTLD("foo.bar.baz") == "baz"
     * @param uri A URI that includes a host part
     * @return the top-level domain
     */
    public static String getTLD(String uri) {
        final String parts[] = getHost(uri).split("\\.");
        if (parts.length > 0) return parts[parts.length-1];
        throw new IllegalArgumentException("Invalid host in URI: "+uri);
    }

    /**
     * getRegisteredDomain("http://foo.bar.baz/...") == "bar.baz"
     * @param url A URI that includes a host part
     * @return the "registered" domain, which includes the TLD and one level up.
     */
    @NonNull public static String getRegisteredDomain(@NonNull final String url) { return hostToDomain(getHost(url)); }

    /**
     * hostToDomain("foo.bar.baz") == "bar.baz"
     * @param host A full host name
     * @return the "registered" domain, which includes the TLD and one level up.
     */
    @NonNull public static String hostToDomain(@NonNull final String host) {
        final var parts = host.split("\\.");
        switch (parts.length) {
            case 0: throw new IllegalArgumentException("Invalid host: " + host);
            case 1: return host;
            default: return parts[parts.length - 2] + "." + parts[parts.length - 1];
        }
    }

    public static String getFile(String uri) {
        final String path = toUri(uri).getPath();
        final int last = path.lastIndexOf('/');
        if (last == -1 || last == path.length()-1) return null;
        return path.substring(last+1);
    }

    public static String getFileExt(String uri) {
        final String path = toUri(uri).getPath();
        final int last = path.lastIndexOf('.');
        if (last == -1 || last == path.length()-1) return null;
        return path.substring(last+1);
    }

    public static boolean isHost(String uriString, String host) {
        return !empty(uriString) && toUri(uriString).getHost().equals(host);
    }

    // adapted from https://stackoverflow.com/a/13592567/1251543
    public static Map<String, String> queryParams(String query) {
        if (empty(query) || empty(query.trim()) || query.trim().equals("?")) return Collections.emptyMap();
        if (query.contains("?")) query = query.substring(query.indexOf("?")+1);
        if (empty(query)) return Collections.emptyMap();
        final Map<String, String> query_pairs = new LinkedHashMap<>();
        final String[] pairs = query.split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? urlDecode(pair.substring(0, idx)) : pair;
            final String value = idx > 0 && pair.length() > idx + 1 ? urlDecode(pair.substring(idx + 1)) : null;
            query_pairs.put(key, value);
        }
        return query_pairs;
    }

    // adapted from https://stackoverflow.com/a/13592567/1251543
    public static Map<String, List<String>> queryMultiParams(String query) {
        final Map<String, List<String>> query_pairs = new LinkedHashMap<>();
        final String[] pairs = query.split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? urlDecode(pair.substring(0, idx)) : pair;
            final String value = idx > 0 && pair.length() > idx + 1 ? urlDecode(pair.substring(idx + 1)) : null;
            query_pairs.computeIfAbsent(key, k -> new LinkedList<>()).add(value);
        }
        return query_pairs;
    }

}
