package org.cobbzilla.util.http;

import java.net.URI;
import java.net.URISyntaxException;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class URIUtil {

    public static URI toUri(String uri) {
        try { return new URI(uri); } catch (URISyntaxException e) {
            return die("Invalid URI: " + uri);
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
     * getRegisteredDomain("foo.bar.baz") == "bar.baz"
     * @param uri A URI that includes a host part
     * @return the "registered" domain, which includes the TLD and one level up.
     */
    public static String getRegisteredDomain(String uri) {
        final String host = getHost(uri);
        final String parts[] = host.split("\\.");
        switch (parts.length) {
            case 0: throw new IllegalArgumentException("Invalid host: "+host);
            case 1: return host;
            default: return parts[parts.length-2] + "." + parts[parts.length-1];
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

}
