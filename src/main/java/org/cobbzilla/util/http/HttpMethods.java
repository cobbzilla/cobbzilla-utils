package org.cobbzilla.util.http;

import org.apache.http.client.methods.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class HttpMethods {

    public static final String HEAD = "HEAD";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String PATCH = "PATCH";
    public static final String OPTIONS = "OPTIONS";

    public static HttpRequestBase request(String method, String uri) {
        switch (method) {
            case HttpMethods.HEAD:   return new HttpHead(uri);
            case HttpMethods.GET:    return new HttpGet(uri);
            case HttpMethods.POST:   return new HttpPost(uri);
            case HttpMethods.PUT:    return new HttpPut(uri);
            case HttpMethods.DELETE: return new HttpDelete(uri);
            case HttpMethods.PATCH:  return new HttpPatch(uri);
            case HttpMethods.OPTIONS:return new HttpOptions(uri);
        }
        return die("request: invalid HTTP method: "+method);
    }

}
