package org.cobbzilla.util.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class URIBean {

    @Getter @Setter private String scheme = "http";
    @Getter @Setter private String host;
    @Getter @Setter private int port = 80;
    @Getter @Setter private String path = "/";
    @Getter @Setter private String query;

    public URI toURI() {
        try {
            return new URIBuilder()
                    .setScheme(scheme)
                    .setHost(host)
                    .setPort(port)
                    .setPath(path)
                    .setCustomQuery(query)
                    .build();
        } catch (URISyntaxException e) {
            return die("toURI: "+e, e);
        }
    }

    @JsonIgnore public String getFullPath() { return getPath() + (empty(query) ? "" : "?" + query); }

}
