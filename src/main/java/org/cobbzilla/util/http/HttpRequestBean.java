package org.cobbzilla.util.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.cobbzilla.util.collection.NameAndValue;
import org.cobbzilla.util.string.StringUtil;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.http.HttpContentTypes.NV_HTTP_JSON;
import static org.cobbzilla.util.http.HttpMethods.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.system.CommandShell.execScript;

/**
 * A simple bean class that encapsulates the four things needed to make an HTTP request:
 *   * an HTTP request `method`, like GET, POST, PUT, etc. The default is GET
 *   * a `uri`, this is the only required parameter
 *   * an optional `entity`, representing the request body to send for methods like POST and PUT
 *   * an optional array of `headers`, name/value pairs (allowing duplicates) that will be the HTTP request headers
 */
@NoArgsConstructor @ToString(of={"method", "uri"}) @Accessors(chain=true)
public class HttpRequestBean {

    @Getter @Setter private String method = GET;
    @Getter @Setter private String uri;

    @Getter @Setter private String entity;
    @Getter @Setter private InputStream entityInputStream;
    @Getter @Setter private Boolean discardResponseEntity;
    public boolean discardResponseEntity () { return discardResponseEntity != null && discardResponseEntity; }

    public HttpRequestBean(HttpRequestBean request) { copy(this, request); }

    public boolean hasData () { return entity != null; }
    public boolean hasStream () { return entityInputStream != null; }

    @Getter @Setter private List<NameAndValue> headers = new ArrayList<>();
    public HttpRequestBean withHeader (String name, String value) { setHeader(name, value); return this; }
    public HttpRequestBean setHeader (String name, String value) {
        headers.add(new NameAndValue(name, value));
        return this;
    }
    public boolean hasHeaders () { return !empty(headers); }
    public boolean hasHeader(String name) { return NameAndValue.find(getHeaders(), name) != null; }

    public HttpRequestBean (String uri) { this(GET, uri, null); }

    public HttpRequestBean (String method, String uri) { this(method, uri, null); }

    public HttpRequestBean (String method, String uri, String entity) {
        this.method = method;
        this.uri = uri;
        this.entity = entity;
    }

    public HttpRequestBean (String method, String uri, String entity, List<NameAndValue> headers) {
        this(method, uri, entity);
        this.headers = headers;
    }

    public HttpRequestBean (String method, String uri, String entity, NameAndValue[] headers) {
        this(method, uri, entity);
        this.headers = Arrays.asList(headers);
    }

    public HttpRequestBean (String method, String uri, InputStream entity, String name, NameAndValue[] headers) {
        this(method, uri);
        this.entity = name;
        this.entityInputStream = entity;
        this.headers = new ArrayList(Arrays.asList(headers));
    }

    public Map<String, Object> toMap () {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("method", method);
        map.put("uri", uri);
        if (!empty(headers)) map.put("headers", headers.toArray());
        map.put("entity", hasContentType() ? HttpContentTypes.escape(getContentType().getMimeType(), entity) : entity);
        return map;
    }

    @Getter(lazy=true, value=AccessLevel.PRIVATE) private final URI _uri = initURI();

    private URI initURI() { return StringUtil.uriOrDie(uri); }

    @JsonIgnore public String getHost () { return get_uri().getHost(); }
    @JsonIgnore public int getPort () { return get_uri().getPort(); }
    @JsonIgnore public String getPath () { return get_uri().getPath(); }

    @JsonIgnore @Getter(lazy=true) private final HttpHost httpHost = initHttpHost();
    private HttpHost initHttpHost() { return new HttpHost(getHost(), getPort(), get_uri().getScheme()); }

    @Getter @Setter private HttpAuthType authType;
    @Getter @Setter private String authUsername;
    @Getter @Setter private String authPassword;

    public boolean hasAuth () { return authType != null; }

    public HttpRequestBean setAuth(HttpAuthType authType, String name, String password) {
        setAuthType(authType);
        setAuthUsername(name);
        setAuthPassword(password);
        return this;
    }

    @JsonIgnore public ContentType getContentType() {
        if (!hasHeaders()) return null;
        final String value = getFirstHeaderValue(CONTENT_TYPE);
        if (empty(value)) return null;
        return ContentType.parse(value);
    }
    public boolean hasContentType () { return getContentType() != null; }

    public String contentType () { return hasContentType() ? getContentType().getMimeType() : null; }

    @JsonIgnore public Long getContentLength() {
        if (!hasHeaders()) return null;
        final String value = getFirstHeaderValue(CONTENT_LENGTH);
        if (empty(value)) return null;
        return Long.parseLong(value);
    }
    public boolean hasContentLength () { return getContentLength() != null; }

    private String getFirstHeaderValue(String name) {
        if (!hasHeaders()) return null;
        for (NameAndValue header : getHeaders()) if (header.getName().equalsIgnoreCase(name)) return header.getValue();
        return null;
    }

    public static HttpRequestBean get   (String path)              { return new HttpRequestBean(GET, path); }
    public static HttpRequestBean put   (String path, String json) { return new HttpRequestBean(PUT, path, json); }
    public static HttpRequestBean post  (String path, String json) { return new HttpRequestBean(POST, path, json); }
    public static HttpRequestBean delete(String path)              { return new HttpRequestBean(DELETE, path); }

    public static HttpRequestBean putJson (String path, String json) { return new HttpRequestBean(PUT, path, json, NV_HTTP_JSON); }
    public static HttpRequestBean postJson(String path, String json) { return new HttpRequestBean(POST, path, json, NV_HTTP_JSON); }

    public String cURL () {
        final StringBuilder b = new StringBuilder("curl '"+getUri()).append("'");
        for (NameAndValue header : getHeaders()) {
            final String name = header.getName();
            b.append(" -H '").append(name).append(": ").append(header.getValue()).append("'");
        }
        if (getMethod().equals(PUT) || getMethod().equals(POST)) {
            b.append(" --data-binary '").append(getEntity()).append("'");
        }
        return b.toString();
    }


    public HttpResponseBean curl() {
        return new HttpResponseBean().setStatus(200).setEntityBytes(execScript(cURL()).getBytes());
    }

    public HttpClientBuilder initClientBuilder(HttpClientBuilder clientBuilder) {
        if (!hasAuth()) return clientBuilder;
        final HttpClientContext localContext = HttpClientContext.create();
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(getHost(), getPort()),
                new UsernamePasswordCredentials(getAuthUsername(), getAuthPassword()));

        final AuthCache authCache = new BasicAuthCache();
        final AuthScheme authScheme = getAuthType().newScheme();
        authCache.put(getHttpHost(), authScheme);

        localContext.setAuthCache(authCache);
        clientBuilder.setDefaultCredentialsProvider(credsProvider);
        return clientBuilder;
    }
}
