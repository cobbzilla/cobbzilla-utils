package org.cobbzilla.util.io.handlers.classpath;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

// from https://stackoverflow.com/a/1769454/1251543
public class Handler extends URLStreamHandler {

    /** The classloader to find resources from. */
    private final ClassLoader classLoader;

    public Handler() { this.classLoader = getClass().getClassLoader(); }

    public Handler(ClassLoader classLoader) { this.classLoader = classLoader; }

    @Override protected URLConnection openConnection(URL u) throws IOException {
        final URL resource = classLoader.getResource(u.getPath());
        return resource == null ? null : resource.openConnection();
    }

}