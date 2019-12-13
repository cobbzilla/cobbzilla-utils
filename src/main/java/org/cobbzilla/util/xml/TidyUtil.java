package org.cobbzilla.util.xml;

import lombok.Cleanup;
import org.cobbzilla.util.io.FileUtil;
import org.w3c.dom.*;
import org.w3c.tidy.Tidy;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

public class TidyUtil {

    public static String tidy(File file) { return tidy(file, (TidyHelper[]) null); }

    public static String tidy(File file, TidyHelper... helpers) { return tidy(FileUtil.toStringOrDie(file), helpers); }

    public static String tidy(String html) { return tidy(html, (TidyHelper[]) null); }

    public static String tidy(String html, TidyHelper... helpers) {
        try {
            @Cleanup final ByteArrayInputStream in = new ByteArrayInputStream(html.getBytes());
            @Cleanup final ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (empty(helpers)) {
                parse(in, out, false);
            } else {
                final Tidy tidy = createTidy();
                final Document doc = tidy.parseDOM(in, null);
                for (TidyHelper helper : helpers) {
                    helper.process(doc);
                }
                tidy.pprint(doc, out);
            }
            return out.toString();

        } catch (Exception e) {
            return die("tidy: " + e, e);
        }
    }

    public static void parse(InputStream in, OutputStream out, boolean removeScripts) {
        final Tidy tidy = createTidy();
        parse(tidy, in, out, removeScripts);
    }

    public static void parse(Tidy tidy, InputStream in, OutputStream out, boolean removeScripts) {
        if (!removeScripts) {
            tidy.parse(in, out);
        } else {
            final Document doc = tidy.parseDOM(in, null);
            removeElement(doc.getDocumentElement(), "script");
            removeElement(doc.getDocumentElement(), "style");
            removeDuplicateAttributes(doc.getDocumentElement());
            tidy.pprint(doc, out);
        }
    }

    public static void removeDuplicateAttributes(Node parent) {
        if (parent.getNodeType() == Node.ELEMENT_NODE) {
            Element elt = (Element) parent;
            if (parent.getAttributes().getLength() > 0) {
                NamedNodeMap map = elt.getAttributes();
                Set<String> found = new HashSet<String>();
                Set<Attr> toRemove = null;
                for (int i=0; i<map.getLength(); i++) {
                    Attr attr = (Attr) map.item(i);
                    if (found.contains(attr.getNodeName())) {
                        if (toRemove == null) toRemove = new HashSet<Attr>();
                        toRemove.add(attr);
                    } else {
                        found.add(attr.getNodeName());
                    }
                }
                if (toRemove != null) {
                    for (Attr attr : toRemove) {
                        elt.removeAttributeNode(attr);
                    }
                }
            }
            NodeList childNodes = elt.getChildNodes();
            for (int i=0; i<childNodes.getLength(); i++) {
                Node child = childNodes.item(i);
                removeDuplicateAttributes(child);
            }
        }
    }

    public static void removeElement(Node parent, String elementName) {
        List<Node> toRemove = null;
        NodeList childNodes = parent.getChildNodes();
        for (int i=0; i<childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                    child.getNodeName().equalsIgnoreCase(elementName)) {
                if (toRemove == null) toRemove = new ArrayList<Node>();
                toRemove.add(child);
            }
        }
        if (toRemove != null) {
            for (Node dead : toRemove) {
                parent.removeChild(dead);
            }
        }
        childNodes = parent.getChildNodes();
        for (int i=0; i<childNodes.getLength(); i++) {
            removeElement(childNodes.item(i), elementName);
        }
    }

    public static Tidy createTidy() { return createTidy(null); }

    public static Tidy createTidy(Tidy tidyConfig) {
        final Tidy tidy = new Tidy();
        if (tidyConfig != null) copy(tidy, tidyConfig);
        return tidy;
    }
}
