package org.cobbzilla.util.http;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class HttpContentTypeAndCharset {

    @Getter @Setter private String contentType;
    public boolean hasContentType () { return !empty(contentType); }

    public boolean isContentType(String type) {
        return hasContentType() && contentType.equalsIgnoreCase(type);
    }

    @Getter @Setter private String charset;
    public boolean hasCharset () { return !empty(charset); }

    public boolean isCharset(String cs) {
        return hasCharset() && charset.equalsIgnoreCase(cs);
    }

    public HttpContentTypeAndCharset (String val) {
        if (!empty(val)) {
            final int semiPos = val.indexOf(';');
            if (semiPos == -1) {
                contentType = val.trim();
            } else {
                contentType = val.substring(0, semiPos).trim();
                if (semiPos < val.length()-1) {
                    for (String part : val.substring(semiPos + 1).split(";")) {
                        final String[] params = part.split("=");
                        if (params.length == 2 && params[0].equalsIgnoreCase("charset")) {
                            charset = params[1].trim();
                        }
                    }
                }
            }
        }
    }
}
