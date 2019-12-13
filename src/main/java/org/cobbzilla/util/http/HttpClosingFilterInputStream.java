package org.cobbzilla.util.http;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.FilterInputStream;
import java.io.IOException;

public class HttpClosingFilterInputStream extends FilterInputStream {

    private final CloseableHttpClient httpClient;

    public HttpClosingFilterInputStream(CloseableHttpClient httpClient,
                                        CloseableHttpResponse response) throws IOException {
        super(response.getEntity().getContent());
        this.httpClient = httpClient;
    }

    @Override public void close() throws IOException {
        IOException ioe = null;
        try {
            super.close();
        } catch (IOException e) {
            ioe = e;
        }
        httpClient.close();
        if (ioe != null) throw ioe;
    }

}
