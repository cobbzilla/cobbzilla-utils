package org.cobbzilla.util.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.cobbzilla.util.collection.NameAndValue;
import org.cobbzilla.util.json.JsonUtil;

import java.io.*;
import java.util.*;

import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.mkdirOrDie;
import static org.cobbzilla.util.string.StringUtil.UTF8cs;

@Slf4j @Accessors(chain=true) @ToString(of={"status", "headers"})
public class HttpResponseBean {

    public static final HttpResponseBean OK = new HttpResponseBean().setStatus(HttpStatusCodes.OK);

    @Getter @Setter private int status;
    @Getter @Setter private List<NameAndValue> headers;
    @JsonIgnore @Getter private byte[] entity;
    @Getter @Setter private long contentLength;
    @Getter @Setter private String contentType;

    @JsonIgnore public boolean isOk() { return (status / 100) == 2; }

    public Map<String, Object> toMap () {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", status);
        if (!empty(headers)) map.put("headers", headers.toArray());
        map.put("entity", hasContentType() ? HttpContentTypes.escape(contentType(), getEntityString()) : getEntityString());
        return map;
    }

    public boolean hasHeader (String name) { return !empty(getHeaderValues(name)); }
    public boolean hasContentType () { return contentType != null || hasHeader(HttpHeaders.CONTENT_TYPE); }
    public String contentType () { return contentType != null ? contentType : getFirstHeaderValue(HttpHeaders.CONTENT_TYPE); }

    public void addHeader(String name, String value) {
        if (headers == null) headers = new ArrayList<>();
        if (name.equalsIgnoreCase(CONTENT_TYPE)) setContentType(value);
        else if (name.equalsIgnoreCase(CONTENT_LENGTH)) setContentLength(Long.valueOf(value));
        headers.add(new NameAndValue(name, value));
    }

    public HttpResponseBean setEntityBytes(byte[] bytes) { this.entity = bytes; return this; }

    public HttpResponseBean setEntity (InputStream entity) {
        try {
            this.entity = entity == null ? null : IOUtils.toByteArray(entity);
            return this;
        } catch (IOException e) {
            return die("setEntity: error reading stream: " + e, e);
        }
    }

    public boolean hasEntity () { return !empty(entity); }

    public String getEntityString () {
        try {
            return entity == null ? null : new String(entity, UTF8cs);
        } catch (Exception e) {
            log.warn("getEntityString: error parsing bytes: "+e);
            return null;
        }
    }

    public <T> T getEntity (Class<T> clazz) {
        return entity == null ? null : JsonUtil.fromJsonOrDie(getEntityString(), clazz);
    }

    public Collection<String> getHeaderValues (String name) {
        final List<String> values = new ArrayList<>();
        if (!empty(headers)) for (NameAndValue header : headers) if (header.getName().equalsIgnoreCase(name)) values.add(header.getValue());
        return values;
    }


    public String getFirstHeaderValue (String name) {
        if (empty(headers)) return null;
        for (NameAndValue header : headers) if (header.getName().equalsIgnoreCase(name)) return header.getValue();
        return null;
    }

    public HttpResponseBean setHttpHeaders(Header[] headers) {
        for (Header header : headers) {
            addHeader(header.getName(), header.getValue());;
        }
        return this;
    }

    public HttpResponseBean setHttpHeaders(Map<String, List<String>> h) {
        if (empty(h)) return this;
        for (Map.Entry<String, List<String>> e : h.entrySet()) {
            if (!empty(e.getKey())) {
                for (String v : e.getValue()) {
                    if (!empty(v)) addHeader(e.getKey(), v);
                }
            }
        }
        return this;
    }

    public File toFile(File file) {
        if (!isOk()) return die("unexpected HTTP response: "+this);
        if (!file.getParentFile().exists()) mkdirOrDie(file.getParentFile());
        try (OutputStream out = new FileOutputStream(file)) {
            IOUtils.copyLarge(new ByteArrayInputStream(getEntity()), out);
            return file;
        } catch (Exception e) {
            return die("toFile: "+e, e);
        }
    }
}
