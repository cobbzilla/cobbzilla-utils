package org.cobbzilla.util.xml;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.string.StringUtil;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CommonEntityResolver implements EntityResolver {

    private static final String COMMON_DTD_ROOT = StringUtil.getPackagePath(CommonEntityResolver.class);
    private static final String[][] COMMON_ENTITIES = {
            { "-//W3C//DTD XHTML 1.0 Transitional//EN", COMMON_DTD_ROOT+"/xhtml1-transitional.dtd" },
            { "-//W3C//ENTITIES Latin 1 for XHTML//EN", COMMON_DTD_ROOT+"/xhtml-lat1.ent"},
            { "-//W3C//ENTITIES Symbols for XHTML//EN", COMMON_DTD_ROOT+"/xhtml-symbol.ent"},
            { "-//W3C//ENTITIES Special for XHTML//EN", COMMON_DTD_ROOT+"/xhtml-special.ent"}
    };
    private static Map<String, String> COMMON_ENTITY_MAP = new HashMap<String, String>();
    static {
        for (String[] entity : COMMON_ENTITIES) {
            COMMON_ENTITY_MAP.put(entity[0], entity[1]);
        }
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        String resource = COMMON_ENTITY_MAP.get(publicId);
        if (resource == null) {
            String msg = "resolveEntity(" + publicId + ", " + systemId + ") called, returning null";
            log.info(msg);
            System.out.println(msg);
            return null;
        }
        return new InputSource(StreamUtil.loadResourceAsStream(resource));
    }

}
