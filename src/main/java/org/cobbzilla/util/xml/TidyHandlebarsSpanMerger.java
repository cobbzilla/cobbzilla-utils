package org.cobbzilla.util.xml;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.mappy.MappyList;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map;

import static org.cobbzilla.util.handlebars.HandlebarsUtil.HB_END;
import static org.cobbzilla.util.handlebars.HandlebarsUtil.HB_START;

@Slf4j
public class TidyHandlebarsSpanMerger implements TidyHelper {

    public static final TidyHandlebarsSpanMerger instance = new TidyHandlebarsSpanMerger();

    @Override public void process(Document doc) {
        final MappyList<Node, Node> toRemove = new MappyList<>();
        mergeSpans(doc, toRemove);
        for (Map.Entry<Node, Node> n : toRemove.entrySet()) {
            n.getKey().removeChild(n.getValue());
        }
    }

    protected void mergeSpans(Node parent, MappyList<Node, Node> toRemove) {

        Node spanStart = null;
        StringBuilder spanTemp = null;
        NodeList childNodes = parent.getChildNodes();
        for (int i=0; i <childNodes.getLength(); i++) {
            final Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (child.getNodeName().equalsIgnoreCase("span")) {
                    if (spanStart == null) {
                        spanStart = child;
                        spanTemp = new StringBuilder(collectText(child));

                    } else if (sameAttrs(spanStart.getAttributes(), child.getAttributes())) {
                        //noinspection ConstantConditions
                        append(spanTemp, collectText(child));
                        setSpan(spanStart, spanTemp);
                        toRemove.put(parent, child);
                    } else {
                        spanStart = child;
                        spanTemp = new StringBuilder(collectText(child));
                    }
                } else if (child.hasChildNodes()) {
                    mergeSpans(child, toRemove);
                }
                continue;

            } else if (child.getNodeType() == Node.TEXT_NODE) {
                if (spanTemp != null) {
                    append(spanTemp, child.getNodeValue());
                    setSpan(spanStart, spanTemp);
                    toRemove.put(parent, child);
                    continue;
                }
            }
            if (child.hasChildNodes()) {
                mergeSpans(child, toRemove);
            }
        }
        if (spanStart != null && spanTemp != null && spanTemp.length() > 0) {
            setSpan(spanStart, spanTemp);
        }
    }

    private void setSpan(Node spanStart, StringBuilder spanTemp) {
        if (spanTemp == null || spanStart == null || spanStart.getFirstChild() == null) return;
        spanStart.getFirstChild().setNodeValue(spanTemp.toString());
    }

    public static String scrubHandlebars(String text) {
        final StringBuilder b = new StringBuilder();
        int start = 0;
        int pos = text.indexOf(HB_START, start);
        while (pos != -1) {
            b.append(text.substring(start, pos)).append(HB_START);
            start = pos + 2;
            int endPos = text.indexOf(HB_END, start);
            if (endPos == -1) {
                b.append(text.substring(start));
                return b.toString();
            } else {
                b.append(scrubHtmlEntities(text.substring(start, endPos))).append(HB_END);
                start = endPos + 2;
            }
            pos = text.indexOf(HB_START, start);
        }
        if (start != text.length()-1) b.append(text.substring(start));
        return b.toString();
    }

    public static String scrubHtmlEntities(String s) {
        return s.replace("&ldquo;", "\"").replace("&rdquo;", "\"")
                .replace("&lsquo;", "'").replace("&rsquo;", "'");
    }

    private StringBuilder append(StringBuilder b, String s) {
        if (s == null || s.length() == 0) return b;
        return b.append(s);
    }

    private String collectText(Node node) {
        final StringBuilder b = new StringBuilder();
        if (node.hasChildNodes()) {
            final NodeList childNodes = node.getChildNodes();
            for (int i=0; i<childNodes.getLength(); i++) {
                final Node child = childNodes.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    b.append(child.getNodeValue());
                }
            }
        }
        return b.toString();
    }


    private boolean sameAttrs(NamedNodeMap a1, NamedNodeMap a2) {
        if (isGoBack(a1) || isGoBack(a2)) return true;
        if (a1.getLength() != a2.getLength()) return false;
        for (int i=0; i<a1.getLength(); i++) {
            boolean found = false;
            final Node a1item = a1.item(i);
            for (int j=0; j<a2.getLength(); j++) {
                if (a1item.getNodeName().equalsIgnoreCase(a2.item(j).getNodeName())
                        && a1item.getNodeValue().equalsIgnoreCase(a2.item(j).getNodeValue())) {
                    found = true; break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private boolean isGoBack(NamedNodeMap attrs) {
        final Node id = attrs.getNamedItem("id");
        return id != null && id.getNodeValue().equals("_GoBack");
    }
}
