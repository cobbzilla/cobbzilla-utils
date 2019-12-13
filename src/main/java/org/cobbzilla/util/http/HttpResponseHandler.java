package org.cobbzilla.util.http;

public interface HttpResponseHandler {

    boolean isSuccess (HttpRequestBean request, HttpResponseBean response);
    void success (HttpRequestBean request, HttpResponseBean response);
    void failure (HttpRequestBean request, HttpResponseBean response);

}
