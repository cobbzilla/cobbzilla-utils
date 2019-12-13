package org.cobbzilla.util.http;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.cobbzilla.util.reflect.ObjectFactory;

import java.util.Map;

@AllArgsConstructor
public class PooledHttpClientFactory implements ObjectFactory<CloseableHttpClient> {

    @Getter private String host;
    @Getter private int maxConnections;

    @Override public CloseableHttpClient create() {
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxConnections);
        cm.setMaxPerRoute(new HttpRoute(new HttpHost(host)), maxConnections);
        return HttpClients.custom()
                .setConnectionManager(cm)
                .setConnectionManagerShared(true)
                .build();
    }

    @Override public CloseableHttpClient create(Map<String, Object> ctx) { return create(); }

}
