package org.cobbzilla.util.xml;

import org.atteo.xmlcombiner.XmlCombiner;
import org.cobbzilla.util.string.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.system.Bytes.KB;

public class XmlUtil {

    public static final String XML10_UTF8 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    public static final String XML10_UTF8_REGEX = "(<\\?xml [^?]+\\?>)";
    public static final Pattern XML10_UTF8_PATTERN = Pattern.compile(XML10_UTF8_REGEX);

    public static OutputStream merge (OutputStream out, String... documents) {
        try {
            final XmlCombiner combiner = new XmlCombiner();
            for (String document : documents) {
                combiner.combine(StringUtil.stream(document));
            }
            combiner.buildDocument(out);
            return out;

        } catch (Exception e) {
            return die("merge: "+e, e);
        }
    }

    public static String merge (String... documents) { return merge((int) (32*KB), documents); }

    public static String merge (int bufsiz, String... documents) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufsiz);
        return merge(buffer, documents).toString();
    }

    public static String replaceElement (String document, String fromElement, String toElement) {
        return document
                .replaceAll("<\\s*"+fromElement+"([^>]*)>", "<"+toElement+"$1>")
                .replaceAll("</\\s*"+fromElement+"\\s*>", "</"+toElement+">")
                .replaceAll("<\\s*"+fromElement+"\\s*/>", "<"+toElement+"/>");
    }

    public static Element textElement(Document doc, String element, String text) {
        final Element node = doc.createElement(element);
        node.appendChild(doc.createTextNode(text));
        return node;
    }

    public static Document readDocument (String xml) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final InputSource is = new InputSource(new StringReader(xml));
            return builder.parse(is);
        } catch (Exception e) {
            return die("readDocument: "+e, e);
        }
    }

    public static void writeDocument (Document doc, Writer writer) {
        try {
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
        } catch (Exception e) {
            die("writeDocument: " + e, e);
        }
    }

    public static String writeDocument(Document doc) {
        final StringWriter writer = new StringWriter();
        writeDocument(doc, writer);
        return writer.getBuffer().toString();
    }

    public static Node applyRecursively (Element element, XmlElementFunction func) {
        func.apply(element);
        final NodeList childNodes = element.getChildNodes();
        if (childNodes != null && childNodes.getLength() > 0) {
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node item = childNodes.item(i);
                if (item instanceof Element) applyRecursively((Element) item, func);
            }
        }
        return element;
    }

    public static List<Element> findElements(Document doc, final String name) {
        final List<Element> found = new ArrayList<>();
        applyRecursively(doc.getDocumentElement(), new MatchNodeName(name, found));
        return found;
    }

    public static List<Element> findElements(Element element, final String name) {
        final List<Element> found = new ArrayList<>();
        applyRecursively(element, new MatchNodeName(name, found));
        return found;
    }

    public static List<Element> findElements(Element element, final String name, final String content) {
        if (content == null) return findElements(element, name);
        final List<Element> found = new ArrayList<>();
        applyRecursively(element, new MatchNodeNameAndText(name, content, found));
        return found;
    }

    public static List<Element> findElements(Element element, final String name, final Map<String, String> conditions) {
        if (empty(conditions)) return findElements(element, name);
        final List<Element> found = new ArrayList<>();
        applyRecursively(element, new MatchNodeSubElements(name, conditions, found));
        return found;
    }

    public static Element findUniqueElement(Document doc, String name) {
        final List<Element> elements = findElements(doc, name);
        if (empty(elements)) return null;
        if (elements.size() > 1) return die("add: multiple "+name+" elements found");
        return elements.get(0);
    }

    public static Element findFirstElement(Document doc, String name) {
        final List<Element> elements = findElements(doc, name);
        return empty(elements) ? null : elements.get(0);
    }

    public static Element findFirstElement(Element e, String name) {
        final List<Element> elements = findElements(e, name);
        return empty(elements) ? null : elements.get(0);
    }

    public static Element findFirstElement(Element e, String name, String content) {
        final List<Element> elements = findElements(e, name, content);
        return empty(elements) ? null : elements.get(0);
    }

    public static Element findFirstElement(Element e, String name, Map<String, String> conditions) {
        final List<Element> elements = findElements(e, name, conditions);
        return empty(elements) ? null : elements.get(0);
    }

    public static <T> T findLargest(Document doc, final ElementMatcher matcher, final ElementTransformer<T> transformer) {
        final AtomicReference<T> largest = new AtomicReference<>();
        applyRecursively(doc.getDocumentElement(), element -> {
            if (matcher.matches(element)) {
                final T val = transformer.transform(element);
                if (val != null) {
                    synchronized (largest) {
                        final T curVal = largest.get();
                        if (curVal == null || ((Comparable) val).compareTo(curVal) > 0) largest.set(val);
                    }
                }
            }
        });
        return largest.get();
    }

    public static void removeElements(Document doc, String name) {
        for (Element e : XmlUtil.findElements(doc, name)) e.getParentNode().removeChild(e);
    }

    public static boolean same(Node n1, Node n2) {
        if (n1 == n2) return true;
        if (!n1.getNodeName().equals(n2.getNodeName())) return false;
        final String id1 = id(n1);
        final String id2 = id(n2);
        return id1 != null && id2 != null && id1.equals(id2);
    }

    public static String id (Node n) {
        return n.hasAttributes() ? n.getAttributes().getNamedItem("id").getTextContent() : null;
    }

    public static Element getElementById(Document doc, final String id) {
        final AtomicReference<Element> found = new AtomicReference<>();
        applyRecursively(doc.getDocumentElement(), element -> {
            if (id.equals(id(element))) {
                if (found.get() != null) die("multiple elements found with id="+id);
                found.set(element);
            }
        });
        return found.get();
    }

    public static String stripXmlPreamble(String xml) {
        return XML10_UTF8_PATTERN.matcher(xml).replaceAll("");
    }

    private static abstract class MatchNodeXMLFunction implements XmlElementFunction {
        protected final String value;
        protected final List<Element> found;

        public MatchNodeXMLFunction(String value, List<Element> found) {
            if (value == null) die("Value cannot be null");
            this.value = value;
            this.found = found;
        }

        protected abstract boolean check(Element element);

        @Override public void apply(Element element) {
            if (element != null && check(element)) found.add(element);
        }
    }

    private static class MatchNodeName extends MatchNodeXMLFunction {
        public MatchNodeName(String value, List<Element> found) { super(value, found); }

        @Override protected boolean check(Element element) {
            return value.equalsIgnoreCase(element.getNodeName());
        }
    }

    public static class MatchNodeNameAndText extends MatchNodeName {
        private final String text;

        public MatchNodeNameAndText(String name, String text, List<Element> found) {
            super(name, found);
            if (text == null) die("Text cannot be null");
            this.text = text.trim();
        }

        @Override protected boolean check(Element element) {
            return super.check(element) && text.equals(element.getTextContent().trim());
        }
    }

    public static class MatchNodeSubElements extends MatchNodeName {
        private final Map<String, String> conditions;

        public MatchNodeSubElements(String name, Map<String, String> conditions, List<Element> found) {
            super(name, found);
            if (empty(conditions)) die("Conditions map cannot be null");
            this.conditions = conditions;
        }

        @Override protected boolean check(Element element) {
            if (!super.check(element)) return false;
            for (Map.Entry<String, String> condition : conditions.entrySet()) {
                if (findFirstElement(element, condition.getKey(), condition.getValue()) == null) return false;
            }
            return true;
        }
    }
}
