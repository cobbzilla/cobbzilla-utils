package org.cobbzilla.util.io;

import org.cobbzilla.util.collection.StringSetSource;

import java.io.File;
import java.util.Collection;

public class PathListFileResolver extends StringSetSource implements FileResolver {

    public PathListFileResolver(Collection<String> paths) { addValues(paths); }

    @Override public File resolve(String path) {
        for (String val : getValues()) {
            if (!val.endsWith(File.separator)) val += File.separator;
            if (path.startsWith(File.separator)) path = path.substring(File.separator.length());
            final File f = new File(val + path);
            if (f.exists() && f.canRead()) return f;
        }
        return null;
    }

}
