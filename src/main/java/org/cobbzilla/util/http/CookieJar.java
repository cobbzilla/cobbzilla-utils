package org.cobbzilla.util.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.cobbzilla.util.collection.CaseInsensitiveStringKeyMap;

import java.util.*;

@NoArgsConstructor
public class CookieJar extends CaseInsensitiveStringKeyMap<HttpCookieBean> implements CookieStore {

    public CookieJar(List<HttpCookieBean> cookies) { for (HttpCookieBean cookie : cookies) add(cookie); }

    public CookieJar(HttpCookieBean cookie) { add(cookie); }

    public void add (HttpCookieBean cookie) {
        if (cookie.expired()) {
            remove(cookie.getName());
        } else {
            put(cookie.getName(), cookie);
        }
    }

    @JsonIgnore
    public String getRequestValue() {
        final StringBuilder sb = new StringBuilder();
        for (String name : keySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(name).append("=").append(get(name).getValue());
        }
        return sb.toString();
    }

    public List<HttpCookieBean> getCookiesList () { return new ArrayList<>(values()); }

    @Override public void addCookie(Cookie cookie) { add(new HttpCookieBean(cookie)); }

    @Override public List<Cookie> getCookies() {
        final List<Cookie> cookies = new ArrayList<>(size());
        for (HttpCookieBean cookie : values()) {
            cookies.add(cookie.toHttpClientCookie());
        }
        return cookies;
    }

    @Override public boolean clearExpired(Date date) {
        final long expiration = date.getTime();
        final Set<String> toRemove = new HashSet<>();
        for (HttpCookieBean cookie : values()) {
            if (cookie.expired(expiration)) toRemove.add(cookie.getName());
        }
        for (String name : toRemove) remove(name);
        return false;
    }
}