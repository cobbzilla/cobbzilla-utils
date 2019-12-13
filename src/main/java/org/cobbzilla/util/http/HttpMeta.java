package org.cobbzilla.util.http;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileUtil;

import java.io.File;

import static org.cobbzilla.util.io.FileUtil.abs;

@NoArgsConstructor @Accessors(chain=true) @Slf4j
public class HttpMeta {

    public HttpMeta (String url) { this.url = url; }

    @Getter @Setter private String url;
    @Getter @Setter private Long lastModified;
    public boolean hasLastModified () { return lastModified != null; }

    @Getter @Setter private String etag;
    public boolean hasEtag () { return etag != null; }

    public boolean shouldRefresh(File file) {
        if (file == null) return true;
        if (hasLastModified()) return getLastModified() > file.lastModified();
        if (hasEtag()) {
            final File etagFile = new File(abs(file)+".etag");
            if (etagFile.exists()) {
                try {
                    return !FileUtil.toString(etagFile).equals(etag);
                } catch (Exception e) {
                    log.warn("shouldRefresh: "+e);
                    return true;
                }
            }
        }
        return true;
    }
}
