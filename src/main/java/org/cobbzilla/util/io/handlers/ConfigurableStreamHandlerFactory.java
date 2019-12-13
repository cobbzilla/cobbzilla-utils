package org.cobbzilla.util.io.handlers;

import org.cobbzilla.util.io.handlers.classpath.Handler;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

// from https://stackoverflow.com/a/1769454/1251543
public class ConfigurableStreamHandlerFactory implements URLStreamHandlerFactory {

    public static final ConfigurableStreamHandlerFactory INSTANCE = new ConfigurableStreamHandlerFactory();

    private final Map<String, URLStreamHandler> protocolHandlers = new HashMap<>();

    public ConfigurableStreamHandlerFactory() { addAll(); }

    public ConfigurableStreamHandlerFactory(String protocol, URLStreamHandler urlHandler) {
        addHandler(protocol, urlHandler);
    }

    public ConfigurableStreamHandlerFactory addAll () {
        addHandler("classpath", new Handler());
        return this;
    }

    public void addHandler(String protocol, URLStreamHandler urlHandler) { protocolHandlers.put(protocol, urlHandler); }

    public URLStreamHandler createURLStreamHandler(String protocol) { return protocolHandlers.get(protocol); }

}