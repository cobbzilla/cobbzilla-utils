package org.cobbzilla.util.xml;

import lombok.Getter;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.TreeInfo;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.xml.sax.InputSource;

import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class XPath2 {

    public static String matchElement (String element) { return "//*[local-name()='"+element+"']"; }
    public static String matchElements (String elements) { return matchElements(elements.split("/")); }
    public static String matchElements (String... elements) {
        final StringBuilder b = new StringBuilder();
        for (String element : elements) {
            b.append("//*[local-name()='").append(element).append("']");
        }
        return b.toString();
    }

    public static XPath2 xpath(String element) { return new XPath2(matchElements(element)); }
    public static XPath2 xpath(String... elements) { return new XPath2(matchElements(elements)); }

    @Getter(lazy=true) private static final XPathFactory xpathFactory = initXpathFactory();
    private static XPathFactory initXpathFactory() {
        try {
            sysinit();
            return XPathFactory.newInstance(NamespaceConstant.OBJECT_MODEL_SAXON);
        } catch (Exception e) {
            return die("initXpathFactory: "+e, e);
        }
    }

    @Getter(lazy=true) private static final XPath xPath = initXPath();
    private static XPath initXPath() { return getXpathFactory().newXPath(); }

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static void sysinit () {
        synchronized (initialized) {
            if (!initialized.get()) {
                final String name = "javax.xml.xpath.XPathFactory:" + NamespaceConstant.OBJECT_MODEL_SAXON;
                System.setProperty(name, "net.sf.saxon.xpath.XPathFactoryImpl");
                initialized.set(true);
            }
        }
    }

    @Getter private Map<String, Path> xpaths = new HashMap<>();

    public XPath2 (String... expressions) {
        if (empty(expressions)) die("XPath2: no expressions");
        for (String expr : expressions) {
            xpaths.put(expr, new Path(expr));
        }
    }

    public Map<String, String> firstMatches (String xml) {  return firstMatches(new Doc(xml)); }

    public Map<String, String> firstMatches (Doc doc) {
        final Map<String, String> matches = new HashMap<>();
        for (Map.Entry<String, Path> path : xpaths.entrySet()) {
            final String match = path.getValue().firstMatch(doc);
            if (!empty(match)) matches.put(path.getKey(), match);
        }
        return matches;
    }

    public String firstMatch(String xml) {
        switch (xpaths.size()) {
            case 0:  return die("firstMatch: no xpath expressions");
            default: return die("firstMatch: more than one xpath expression");
            case 1:
                final Map<String, String> matches = firstMatches(xml);
                return empty(matches) ? null : matches.values().iterator().next();
        }
    }

    public static class Path {

        @Getter private XPathExpression expr;

        public Path (String xpath) {
            try {
                expr = getXPath().compile(xpath);
            } catch (Exception e) {
                die("XPath2.Path: "+e, e);
            }
        }

        public String firstMatch (String xml) {  return firstMatch(new Doc(xml)); }

        public String firstMatch(Doc doc) {
            try {
                final List matches = (List) expr.evaluate(doc.getDoc(), XPathConstants.NODESET);
                if (empty(matches) || matches.get(0) == null) return null;
                final NodeInfo line = (NodeInfo) matches.get(0);
                return line.iterate().next().getStringValue();

            } catch (Exception e) {
                return die("firstMatch: "+e, e);
            }
        }
    }

    public static class Doc {
        @Getter private TreeInfo doc;
        public Doc (String xml) {
            final InputSource is = new InputSource(new ByteArrayInputStream(xml.getBytes()));
            final SAXSource ss = new SAXSource(is);
            final Configuration config = ((XPathFactoryImpl) getXpathFactory()).getConfiguration();
            try {
                doc = config.buildDocumentTree(ss);
            } catch (Exception e) {
                die("XPath2.Doc: "+e, e);
            }
        }
    }
}
