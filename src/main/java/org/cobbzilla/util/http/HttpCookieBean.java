package org.cobbzilla.util.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;
import java.util.StringTokenizer;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

@NoArgsConstructor @Accessors(chain=true) @Slf4j
public class HttpCookieBean {

    public static final DateTimeFormatter[] EXPIRES_PATTERNS = {
            DateTimeFormat.forPattern("E, dd MMM yyyy HH:mm:ss Z"),
            DateTimeFormat.forPattern("E, dd-MMM-yyyy HH:mm:ss Z"),
            DateTimeFormat.forPattern("E, dd MMM yyyy HH:mm:ss z"),
            DateTimeFormat.forPattern("E, dd-MMM-yyyy HH:mm:ss z")
    };

    @Getter @Setter private String name;
    @Getter @Setter private String value;
    @Getter @Setter private String domain;
    public boolean hasDomain () { return !empty(domain); }

    @Getter @Setter private String path;
    @Getter @Setter private String expires;
    @Getter @Setter private Long maxAge;
    @Getter @Setter private boolean secure;
    @Getter @Setter private boolean httpOnly;

    public HttpCookieBean(String name, String value) { this(name, value, null); }

    public HttpCookieBean(String name, String value, String domain) {
        this.name = name;
        this.value = value;
        this.domain = domain;
    }

    public HttpCookieBean (HttpCookieBean other) { copy(this, other); }

    public HttpCookieBean(Cookie cookie) {
        this(cookie.getName(), cookie.getValue(), cookie.getDomain());
        path = cookie.getPath();
        secure = cookie.isSecure();
        final Date expiryDate = cookie.getExpiryDate();
        if (expiryDate != null) {
            expires = EXPIRES_PATTERNS[0].print(expiryDate.getTime());
        }
    }

    public static HttpCookieBean parse (String setCookie) {
        final HttpCookieBean cookie = new HttpCookieBean();
        final StringTokenizer st = new StringTokenizer(setCookie, ";");
        while (st.hasMoreTokens()) {
            final String token = st.nextToken().trim();
            if (cookie.name == null) {
                // first element is the name=value
                final String[] parts = token.split("=");
                cookie.name = parts[0];
                cookie.value = parts.length == 1 ? "" : parts[1];

            } else if (token.contains("=")) {
                final String[] parts = token.split("=");
                switch (parts[0].toLowerCase()) {
                    case "path":    cookie.path = parts[1]; break;
                    case "domain":  cookie.domain = parts[1]; break;
                    case "expires": cookie.expires = parts[1]; break;
                    case "max-age": cookie.maxAge = Long.valueOf(parts[1]); break;
                    default: log.warn("Unrecognized cookie attribute: "+parts[0]);
                }
            } else {
                switch (token.toLowerCase()) {
                    case "httponly": cookie.httpOnly = true; break;
                    case "secure": cookie.secure = true; break;
                    default: log.warn("Unrecognized cookie attribute: "+token);
                }
            }
        }
        return cookie;
    }

    public String toHeaderValue () {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        if (!empty(expires)) sb.append("; Expires=").append(expires);
        if (maxAge != null) sb.append("; Max-Age=").append(maxAge);
        if (!empty(path)) sb.append("; Path=").append(path);
        if (!empty(domain)) sb.append("; Domain=").append(domain);
        if (httpOnly) sb.append("; HttpOnly");
        if (secure) sb.append("; Secure");
        return sb.toString();
    }

    public String toRequestHeader () { return name + "=" + value; }

    public boolean expired () {
        return (maxAge != null && maxAge <= 0)
                || (expires != null && getExpiredDateTime().isBeforeNow());
    }

    public boolean expired (long expiration) {
        return (maxAge != null && now() + maxAge < expiration)
                || (expires != null && getExpiredDateTime().isBefore(expiration));
    }

    @JsonIgnore public Date getExpiryDate () {
        if (maxAge != null) return new Date(now() + maxAge);
        if (expires != null) return getExpiredDateTime().toDate();
        return null;
    }

    protected DateTime getExpiredDateTime() {
        if (empty(expires)) {
            return null;
        }
        for (DateTimeFormatter formatter : EXPIRES_PATTERNS) {
            try {
                return formatter.parseDateTime(expires);
            } catch (Exception ignored) {}
        }
        return die("getExpiredDateTime: unparseable 'expires' value for cookie "+name+": '"+expires+"'");
    }

    public Cookie toHttpClientCookie() {
        final BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setExpiryDate(getExpiryDate());
        cookie.setPath(path);
        cookie.setDomain(domain);
        cookie.setSecure(secure);
        return cookie;
    }
}
